// 文件路径: src/main/java/org/example/nursingtrainingbackend/modules/category/service/impl/CategoryServiceImpl.java
package org.example.nursingtrainingbackend.modules.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.event.CacheEvictionEvent;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.category.dto.CategoryCreateRequest;
import org.example.nursingtrainingbackend.modules.category.dto.CategoryEditRequest;
import org.example.nursingtrainingbackend.modules.category.dto.CategoryStatusRequest;
import org.example.nursingtrainingbackend.modules.category.dto.CategoryTreeQuery;
import org.example.nursingtrainingbackend.modules.category.entity.Category;
import org.example.nursingtrainingbackend.modules.category.mapper.CategoryMapper;
import org.example.nursingtrainingbackend.modules.category.service.CategoryService;
import org.example.nursingtrainingbackend.modules.category.vo.*;
import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;
    private final CourseMapper courseMapper;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(CategoryServiceImpl.class);
    private static final String TREE_CACHE_PREFIX = "nursing:category:tree:v1:";
    private static final String OVERVIEW_CACHE_KEY = "nursing:category:overview:v1";
    private static final long CACHE_TTL_MINUTES = 10;
    /** 获取分类树。 */


    @Override
    public CategoryTreeResult getTree(CategoryTreeQuery query) {
        String cacheKey = TREE_CACHE_PREFIX + query.status() + ":" + (query.keyword() != null ? query.keyword() : "");

        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, CategoryTreeResult.class);
            }
        } catch (Exception e) {
            log.warn("读取分类树缓存失败, key={}", cacheKey, e);
        }

        log.info("[CategoryTree] 查询分类树，入参 query={}", query);

        List<Category> allCategories = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery().orderByDesc(Category::getUpdatedAt, Category::getId)
        );

        log.info("[CategoryTree] 从数据库查询到总条数={}", allCategories.size());


        if (allCategories.isEmpty()) {
            log.warn("[CategoryTree] 数据库无任何分类数据，直接返回空树");
            return new CategoryTreeResult(List.of(), 0L);
        }

        // ===== 调试用：打印前 10 条数据 =====
        allCategories.stream().limit(10).forEach(c ->
                log.debug("[CategoryTree] 原始分类 id={}, name={}, parentId={}, status={}",
                        c.getId(), c.getName(), c.getParentId(), c.getStatus())
        );

        Map<Long, Category> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));

        Map<Long, List<Category>> childrenMap = allCategories.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0L)
                .collect(Collectors.groupingBy(Category::getParentId));

        Set<Long> hasChildrenSet = new HashSet<>(childrenMap.keySet());

        boolean hasFilter = StringUtils.isNotBlank(query.keyword()) || query.status() != null;
        log.info("[CategoryTree] 是否启用过滤 hasFilter={}", hasFilter);


        Set<Long> matchedIds = new HashSet<>();
        if (hasFilter) {
            for (Category c : allCategories) {
                boolean match = true;
                if (StringUtils.isNotBlank(query.keyword())) {
                    match = c.getName().contains(query.keyword());
                }
                if (query.status() != null) {
                    match = match && query.status().equals(c.getStatus());
                }
                if (match) {
                    matchedIds.add(c.getId());
                }
            }

            log.info("[CategoryTree] 过滤后命中的分类ID={}", matchedIds);

            if (matchedIds.isEmpty()) {
                log.warn("[CategoryTree] 过滤后无任何匹配数据，返回空树");
                return new CategoryTreeResult(List.of(), 0L);
            }

            Set<Long> keepIds = new HashSet<>(matchedIds);

            for (Long id : matchedIds) {
                Long cur = id;
                while (cur != null && cur != 0L) {
                    keepIds.add(cur);
                    Category c = categoryMap.get(cur);
                    cur = (c != null) ? c.getParentId() : null;
                }
            }

            Queue<Long> queue = new LinkedList<>(matchedIds);
            while (!queue.isEmpty()) {
                Long id = queue.poll();
                List<Category> children = childrenMap.get(id);
                if (children != null) {
                    for (Category child : children) {
                        if (keepIds.add(child.getId())) {
                            queue.add(child.getId());
                        }
                    }
                }
            }

            allCategories = allCategories.stream()
                    .filter(c -> keepIds.contains(c.getId()))
                    .toList();

            log.info("[CategoryTree] 补齐父子节点后剩余分类数={}", allCategories.size());
        }

        Long rootParentId = (query.parentId() != null && query.parentId() != 0L)
                ? query.parentId() : 0L;
        log.info("[CategoryTree] 使用的 rootParentId={}", rootParentId);

        // 批量查询各类别关联的课程数（一次SQL，避免N+1）
        Map<Long, Long> directCourseCountMap = computeDirectCourseCounts();
        Map<Long, Long> courseCountMap = computeRecursiveCourseCounts(allCategories, directCourseCountMap);

        Map<Long, CategoryNode> nodeMap = new LinkedHashMap<>();
        for (Category c : allCategories) {
            String parentName = null;
            if (c.getParentId() != null && c.getParentId() != 0L) {
                Category parent = categoryMap.get(c.getParentId());
                if (parent != null) {
                    parentName = parent.getName();
                }
            }
            boolean hasChildren = hasChildrenSet.contains(c.getId());
            long directCount = directCourseCountMap.getOrDefault(c.getId(), 0L);
            long totalCount = courseCountMap.getOrDefault(c.getId(), 0L);
            nodeMap.put(c.getId(), CategoryNode.from(c, parentName, hasChildren, directCount, totalCount));
        }

        List<CategoryNode> roots = new ArrayList<>();
        for (Category c : allCategories) {
            CategoryNode node = nodeMap.get(c.getId());
            Long pid = c.getParentId();
            if (Objects.equals(pid, rootParentId)) {
                roots.add(node);
            } else if (pid != null && pid != 0L) {
                CategoryNode parentNode = nodeMap.get(pid);
                if (parentNode != null) {
                    parentNode.children().add(node);
                }
            }
        }

        log.info("[CategoryTree] 最终返回的树根节点数={}, 总节点数={}", roots.size(), nodeMap.size());

        long total = nodeMap.size();
        CategoryTreeResult result = new CategoryTreeResult(roots, total);

        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入分类树缓存失败, key={}", cacheKey, e);
        }

        return result;
    }
    /** 获取详情。 */

    @Override
    public CategoryVO getDetail(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        String parentName = null;
        if (category.getParentId() != null && category.getParentId() != 0L) {
            Category parent = categoryMapper.selectById(category.getParentId());
            if (parent != null) {
                parentName = parent.getName();
            }
        }

        long directCount = courseMapper.selectCount(Wrappers.<Course>lambdaQuery()
                .eq(Course::getCategoryId, id));
        long totalCount = computeRecursiveCourseCount(id);

        return CategoryVO.from(category, parentName, directCount, totalCount);
    }
    /** 创建分类。 */

    @Override
    public CategoryVO create(CategoryCreateRequest request) {
        Long parentId = request.parentId() != null ? request.parentId() : 0L;
        int level = 1;

        if (parentId != 0L) {
            Category parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "父类别不存在");
            }
            if (!Integer.valueOf(1).equals(parent.getStatus())) {
                throw new BusinessException(ErrorCode.CATEGORY_PARENT_DISABLED);
            }
            level = parent.getLevel() + 1;
        }

        Long count = categoryMapper.selectCount(Wrappers.<Category>lambdaQuery()
                .eq(Category::getName, request.name())
                .eq(Category::getParentId, parentId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }

        Category category = new Category();
        category.setName(request.name());
        category.setParentId(parentId);
        category.setLevel(level);
        category.setIcon(request.icon());
        category.setStatus(request.status() != null ? request.status() : 1);
        category.setSort(0);
        categoryMapper.insert(category);

        String parentName = null;
        if (parentId != 0L) {
            Category parent = categoryMapper.selectById(parentId);
            parentName = parent.getName();
        }
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.CATEGORY_TREE));
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.CATEGORY_OVERVIEW));


        return CategoryVO.from(category, parentName, 0L, 0L);
    }
    /** 更新分类。 */

    @Override
    @Transactional
    public CategoryEditVO update(Long id, CategoryEditRequest request) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        Long newParentId = request.parentId() != null ? request.parentId() : 0L;
        boolean cascade = request.cascade() == null || request.cascade();

        if (newParentId != 0L) {
            Category parent = categoryMapper.selectById(newParentId);
            if (parent == null) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "父类别不存在");
            }
        }

        Long nameConflictCount = categoryMapper.selectCount(Wrappers.<Category>lambdaQuery()
                .eq(Category::getName, request.name())
                .eq(Category::getParentId, newParentId)
                .ne(Category::getId, id));
        if (nameConflictCount > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }

        if (newParentId.equals(id)) {
            throw new BusinessException(ErrorCode.CATEGORY_INVALID_PARENT);
        }
        List<Category> descendants = findDescendants(id);
        if (newParentId != 0L && descendants.stream().anyMatch(d -> d.getId().equals(newParentId))) {
            throw new BusinessException(ErrorCode.CATEGORY_INVALID_PARENT);
        }

        int newLevel = (newParentId == 0L) ? 1 : categoryMapper.selectById(newParentId).getLevel() + 1;
        int levelDelta = newLevel - category.getLevel();

        int oldStatus = category.getStatus();
        int newStatus = request.status();
        int affectedCount = 0;

        if (oldStatus == 1 && newStatus == 0) {
            if (cascade) {
                List<Long> descendantIds = descendants.stream().filter(d -> Integer.valueOf(1).equals(d.getStatus())).map(Category::getId).toList();
                if (!descendantIds.isEmpty()) {
                    categoryMapper.update(null, Wrappers.<Category>lambdaUpdate()
                            .in(Category::getId, descendantIds)
                            .set(Category::getStatus, 0)
                            .set(Category::getUpdatedAt, LocalDateTime.now()));
                    affectedCount = descendantIds.size();
                }
            } else {
                boolean hasEnabledDesc = descendants.stream().anyMatch(d -> Integer.valueOf(1).equals(d.getStatus()));
                if (hasEnabledDesc) {
                    throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
                }
            }
        } else if (oldStatus == 0 && newStatus == 1) {
            checkAncestorsEnabled(newParentId);
        }

        category.setName(request.name());
        category.setParentId(newParentId);
        category.setLevel(newLevel);
        category.setIcon(request.icon());
        category.setStatus(newStatus);
        categoryMapper.updateById(category);

        if (levelDelta != 0 && !descendants.isEmpty()) {
            categoryMapper.update(null, Wrappers.<Category>lambdaUpdate()
                    .in(Category::getId, descendants.stream().map(Category::getId).toList())
                    .setSql("level = level + " + levelDelta)
                    .set(Category::getUpdatedAt, LocalDateTime.now()));
        }
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.CATEGORY_TREE));
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.CATEGORY_OVERVIEW));

        return CategoryEditVO.from(category, affectedCount);
    }
    /** 更新状态。 */

    @Override
    @Transactional
    public CategoryStatusVO updateStatus(Long id, CategoryStatusRequest request) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        int newStatus = request.status();
        boolean cascade = request.cascade() == null || request.cascade();
        int affectedCount = 0;

        if (newStatus == 1) {
            checkAncestorsEnabled(category.getParentId());
        }

        if (category.getStatus() == 1 && newStatus == 0) {
            List<Category> descendants = findDescendants(id);
            if (cascade) {
                List<Long> descendantIds = descendants.stream().filter(d -> Integer.valueOf(1).equals(d.getStatus())).map(Category::getId).toList();
                if (!descendantIds.isEmpty()) {
                    categoryMapper.update(null, Wrappers.<Category>lambdaUpdate()
                            .in(Category::getId, descendantIds)
                            .set(Category::getStatus, 0)
                            .set(Category::getUpdatedAt, LocalDateTime.now()));
                    affectedCount = descendantIds.size();
                }
            } else {
                boolean hasEnabledDesc = descendants.stream().anyMatch(d -> Integer.valueOf(1).equals(d.getStatus()));
                if (hasEnabledDesc) {
                    throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
                }
            }
        }

        category.setStatus(newStatus);
        categoryMapper.updateById(category);
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.CATEGORY_TREE));
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.CATEGORY_OVERVIEW));

        return new CategoryStatusVO(id, newStatus, affectedCount);
    }
    /** 删除分类。 */

    @Override
    public void delete(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        Long childCount = categoryMapper.selectCount(Wrappers.<Category>lambdaQuery()
                .eq(Category::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }

        Long courseCount = courseMapper.selectCount(Wrappers.<Course>lambdaQuery()
                .eq(Course::getCategoryId, id));
        if (courseCount > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_COURSES);
        }

        categoryMapper.deleteById(id);
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.CATEGORY_TREE));
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.CATEGORY_OVERVIEW));
    }
    /** 批量删除分类。 */

    @Override
    @Transactional
    public BatchDeleteVO batchDelete(List<Long> ids) {
        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(ids));
        int requestedCount = uniqueIds.size();

        List<Category> categories = categoryMapper.selectBatchIds(uniqueIds);
        if (categories.size() != uniqueIds.size()) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        Map<Long, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
        Set<Long> idSet = new HashSet<>(uniqueIds);

        Map<Long, List<Category>> allChildrenMap = categoryMapper.selectList(Wrappers.emptyWrapper())
                .stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0L)
                .collect(Collectors.groupingBy(Category::getParentId));

        for (Category category : categories) {
            List<Category> children = allChildrenMap.getOrDefault(category.getId(), List.of());
            for (Category child : children) {
                if (!idSet.contains(child.getId())) {
                    throw new BusinessException(ErrorCode.CATEGORY_PARENT_DISABLED,
                            "类别[" + category.getName() + "]存在未包含在删除列表中的子类别[" + child.getName() + "]");
                }
            }

            Long courseCount = courseMapper.selectCount(Wrappers.<Course>lambdaQuery()
                    .eq(Course::getCategoryId, category.getId()));
            if (courseCount > 0) {
                throw new BusinessException(ErrorCode.CATEGORY_HAS_COURSES,
                        "类别[" + category.getName() + "]已关联课程，不能删除");
            }
        }

        categoryMapper.deleteBatchIds(uniqueIds);
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.CATEGORY_TREE));
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.CATEGORY_OVERVIEW));
        return new BatchDeleteVO(requestedCount, uniqueIds.size());
    }
    /** 获取业务概览统计。 */

    @Override
    public CategoryOverviewResult getOverview() {
        try {
            String cached = redisTemplate.opsForValue().get(OVERVIEW_CACHE_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, CategoryOverviewResult.class);
            }
        } catch (Exception e) {
            log.warn("读取分类概览缓存失败", e);
        }

        List<Category> allCategories = categoryMapper.selectList(Wrappers.emptyWrapper());

        long totalCategories = allCategories.size();
        long enabledCategories = allCategories.stream()
                .filter(c -> Integer.valueOf(1).equals(c.getStatus())).count();
        long disabledCategories = totalCategories - enabledCategories;

        // 批量查询各类别关联的课程数
        Map<Long, Long> directCourseCountMap = computeDirectCourseCounts();
        Map<Long, Long> courseCountMap = computeRecursiveCourseCounts(allCategories, directCourseCountMap);

        long totalCourses = directCourseCountMap.values().stream().mapToLong(Long::longValue).sum();
        CategorySummary summary = new CategorySummary(totalCategories, enabledCategories, disabledCategories, totalCourses);

        Map<Long, Category> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));

        List<Category> topCategories = allCategories.stream()
                .sorted(Comparator.comparingLong(Category::getId))
                .limit(5)
                .toList();
        List<TopCategoryItem> topItems = new ArrayList<>();
        for (int i = 0; i < topCategories.size(); i++) {
            Category c = topCategories.get(i);
            long count = courseCountMap.getOrDefault(c.getId(), 0L);
            topItems.add(new TopCategoryItem(c.getId(), c.getName(), count, i + 1));
        }

        List<RecentUpdateItem> recentUpdates = allCategories.stream()
                .sorted(Comparator.comparing(Category::getUpdatedAt).reversed()
                        .thenComparing(Comparator.comparingLong(Category::getId).reversed()))
                .limit(5)
                .map(c -> new RecentUpdateItem(c.getId(), buildCategoryPath(c, categoryMap), c.getUpdatedAt()))
                .toList();

        CategoryOverviewResult result = new CategoryOverviewResult(summary, topItems, recentUpdates);

        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(OVERVIEW_CACHE_KEY, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入分类概览缓存失败", e);
        }

        return result;
    }

    private String buildCategoryPath(Category category, Map<Long, Category> categoryMap) {
        LinkedList<String> parts = new LinkedList<>();
        Category cur = category;
        while (cur != null) {
            parts.addFirst(cur.getName());
            if (cur.getParentId() == null || cur.getParentId() == 0L) {
                break;
            }
            cur = categoryMap.get(cur.getParentId());
        }
        return String.join(" > ", parts);
    }

    private void checkAncestorsEnabled(Long parentId) {
        Long cur = parentId;
        while (cur != null && cur != 0L) {
            Category ancestor = categoryMapper.selectById(cur);
            if (ancestor != null && !Integer.valueOf(1).equals(ancestor.getStatus())) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            }
            cur = (ancestor != null) ? ancestor.getParentId() : null;
        }
    }

    private List<Category> findDescendants(Long parentId) {
        List<Category> all = categoryMapper.selectList(Wrappers.emptyWrapper());
        Map<Long, List<Category>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0L)
                .collect(Collectors.groupingBy(Category::getParentId));
        List<Category> result = new ArrayList<>();
        Queue<Long> queue = new LinkedList<>();
        queue.add(parentId);
        while (!queue.isEmpty()) {
            Long pid = queue.poll();
            List<Category> children = childrenMap.get(pid);
            if (children != null) {
                for (Category child : children) {
                    result.add(child);
                    queue.add(child.getId());
                }
            }
        }
        return result;
    }

    /**
     * 批量查询每个类别直接关联的课程数（一次 SQL）
     */
    private Map<Long, Long> computeDirectCourseCounts() {
        List<Course> allCourses = courseMapper.selectList(
                Wrappers.<Course>lambdaQuery().select(Course::getCategoryId)
        );
        return allCourses.stream()
                .filter(Objects::nonNull)
                .filter(c -> c.getCategoryId() != null)
                .collect(Collectors.groupingBy(Course::getCategoryId, Collectors.counting()));
    }

    /**
     * 基于直接课程数，递归计算每个类别（含子类别）的总课程数
     */
    private Map<Long, Long> computeRecursiveCourseCounts(List<Category> allCategories,
                                                          Map<Long, Long> directCourseCountMap) {
        Map<Long, Long> result = new HashMap<>();
        Map<Long, List<Category>> childrenMap = allCategories.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0L)
                .collect(Collectors.groupingBy(Category::getParentId));

        for (Category c : allCategories) {
            result.put(c.getId(), sumCourseCount(c.getId(), childrenMap, directCourseCountMap, new HashMap<>()));
        }
        return result;
    }

    private long sumCourseCount(Long categoryId, Map<Long, List<Category>> childrenMap,
                                 Map<Long, Long> directCourseCountMap, Map<Long, Long> cache) {
        if (cache.containsKey(categoryId)) return cache.get(categoryId);
        long count = directCourseCountMap.getOrDefault(categoryId, 0L);
        List<Category> children = childrenMap.getOrDefault(categoryId, List.of());
        for (Category child : children) {
            count += sumCourseCount(child.getId(), childrenMap, directCourseCountMap, cache);
        }
        cache.put(categoryId, count);
        return count;
    }

    /**
     * 计算单个类别（含子类别）的总课程数
     */
    private long computeRecursiveCourseCount(Long categoryId) {
        List<Category> allCategories = categoryMapper.selectList(Wrappers.emptyWrapper());
        Map<Long, Long> directCourseCountMap = computeDirectCourseCounts();
        Map<Long, List<Category>> childrenMap = allCategories.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0L)
                .collect(Collectors.groupingBy(Category::getParentId));
        return sumCourseCount(categoryId, childrenMap, directCourseCountMap, new HashMap<>());
    }
}
