package org.example.nursingtrainingbackend.modules.course.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.course.entity.CoursePoint;
import org.example.nursingtrainingbackend.modules.course.entity.CoursePointArticle;
import org.example.nursingtrainingbackend.modules.course.entity.CoursePointPpt;
import org.example.nursingtrainingbackend.modules.course.entity.CoursePointVideo;
import org.example.nursingtrainingbackend.modules.course.mapper.CoursePointArticleMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CoursePointMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CoursePointPptMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CoursePointVideoMapper;
import org.example.nursingtrainingbackend.modules.course.service.CoursePointService;
import org.example.nursingtrainingbackend.modules.course.vo.CoursePointDetailVO;
import org.example.nursingtrainingbackend.modules.courseware.article.entity.Article;
import org.example.nursingtrainingbackend.modules.courseware.article.mapper.ArticleMapper;
import org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt;
import org.example.nursingtrainingbackend.modules.courseware.ppt.mapper.PptMapper;
import org.example.nursingtrainingbackend.modules.courseware.video.entity.Video;
import org.example.nursingtrainingbackend.modules.courseware.video.mapper.VideoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CoursePointServiceImpl implements CoursePointService {

    @Autowired
    private CoursePointMapper coursePointMapper;

    @Autowired
    private CoursePointArticleMapper coursePointArticleMapper;

    @Autowired
    private CoursePointVideoMapper coursePointVideoMapper;

    @Autowired
    private CoursePointPptMapper coursePointPptMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private PptMapper pptMapper;

    @Override
    public CoursePointDetailVO getPointDetail(Long courseId, Long chapterId, Long pointId) {
        // 查询课程点
        CoursePoint point = coursePointMapper.selectById(pointId);
        if (point == null || !point.getCourseId().equals(courseId) || !point.getChapterId().equals(chapterId)) {
            throw new BusinessException(ErrorCode.COURSE_POINT_NOT_FOUND);
        }

        CoursePointDetailVO vo = new CoursePointDetailVO();
        vo.setId(point.getId());
        vo.setCourseId(point.getCourseId());
        vo.setChapterId(point.getChapterId());
        vo.setTitle(point.getTitle());
        vo.setDescription(point.getDescription());
        vo.setRequired(Integer.valueOf(1).equals(point.getRequired()));
        vo.setSort(point.getSort());

        // 关联的文章
        vo.setArticles(buildArticleItems(pointId));

        // 关联的视频
        vo.setVideos(buildVideoItems(pointId));

        // 关联的 PPT
        vo.setPpts(buildPptItems(pointId));

        return vo;
    }

    private List<CoursePointDetailVO.MediaItem> buildArticleItems(Long pointId) {
        List<CoursePointDetailVO.MediaItem> items = new ArrayList<>();
        List<CoursePointArticle> junctions = coursePointArticleMapper.selectList(
                Wrappers.<CoursePointArticle>lambdaQuery()
                        .eq(CoursePointArticle::getCoursePointId, pointId)
                        .orderByAsc(CoursePointArticle::getSort)
                        .orderByAsc(CoursePointArticle::getCreatedAt));
        for (CoursePointArticle junction : junctions) {
            Article article = articleMapper.selectById(junction.getArticleId());
            if (article == null) continue;
            CoursePointDetailVO.MediaItem item = new CoursePointDetailVO.MediaItem();
            item.setId(article.getId());
            item.setTitle(article.getTitle());
            item.setStatus(statusLabel(article.getStatus()));
            items.add(item);
        }
        return items;
    }

    private List<CoursePointDetailVO.MediaItem> buildVideoItems(Long pointId) {
        List<CoursePointDetailVO.MediaItem> items = new ArrayList<>();
        List<CoursePointVideo> junctions = coursePointVideoMapper.selectList(
                Wrappers.<CoursePointVideo>lambdaQuery()
                        .eq(CoursePointVideo::getCoursePointId, pointId)
                        .orderByAsc(CoursePointVideo::getSort)
                        .orderByAsc(CoursePointVideo::getCreatedAt));
        for (CoursePointVideo junction : junctions) {
            Video video = videoMapper.selectById(junction.getVideoId());
            if (video == null) continue;
            CoursePointDetailVO.MediaItem item = new CoursePointDetailVO.MediaItem();
            item.setId(video.getId());
            item.setTitle(video.getTitle());
            item.setDuration(video.getDuration());
            item.setStatus(statusLabel(video.getStatus()));
            items.add(item);
        }
        return items;
    }

    private List<CoursePointDetailVO.MediaItem> buildPptItems(Long pointId) {
        List<CoursePointDetailVO.MediaItem> items = new ArrayList<>();
        List<CoursePointPpt> junctions = coursePointPptMapper.selectList(
                Wrappers.<CoursePointPpt>lambdaQuery()
                        .eq(CoursePointPpt::getCoursePointId, pointId)
                        .orderByAsc(CoursePointPpt::getSort)
                        .orderByAsc(CoursePointPpt::getCreatedAt));
        for (CoursePointPpt junction : junctions) {
            Ppt ppt = pptMapper.selectById(junction.getPptId());
            if (ppt == null) continue;
            CoursePointDetailVO.MediaItem item = new CoursePointDetailVO.MediaItem();
            item.setId(ppt.getId());
            item.setTitle(ppt.getTitle());
            item.setPageCount(ppt.getPageCount());
            item.setStatus(statusLabel(ppt.getStatus()));
            items.add(item);
        }
        return items;
    }

    private String statusLabel(Integer status) {
        if (status == null) return "DRAFT";
        return switch (status) {
            case 1 -> "PUBLISHED";
            case 2 -> "ARCHIVED";
            default -> "DRAFT";
        };
    }
}
