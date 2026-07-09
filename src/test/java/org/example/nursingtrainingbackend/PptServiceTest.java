package org.example.nursingtrainingbackend;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt;
import org.example.nursingtrainingbackend.modules.ppt.dto.CreatePptRequest;
import org.example.nursingtrainingbackend.modules.ppt.dto.UpdatePptRequest;
import org.example.nursingtrainingbackend.modules.ppt.service.PptService;
import org.example.nursingtrainingbackend.modules.ppt.vo.PptDetailVO;
import org.example.nursingtrainingbackend.modules.ppt.vo.PptListItem;
import org.example.nursingtrainingbackend.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PptServiceTest {

    @Autowired
    private PptService pptService;

    // 模拟管理员登录用户
    private AuthenticatedUser mockAdminUser() {
        return new AuthenticatedUser(
                2L,
                "root",
                "系统管理员",
                "ADMIN"
        );
    }

    // 1. 新建PPT
    @Test
    void testCreatePpt() {
        CreatePptRequest req = new CreatePptRequest();
        req.setTitle("单元测试PPT");
        req.setDescription("JUnit自动化测试专用PPT");
        req.setOriginalUrl("https://oss-cn-beijing.aliyuncs.com/shared-doc-backend/ppts/originals/test-uuid.pptx");
        req.setOriginalName("单元测试PPT.pptx");
        req.setFileSize(102400L);
        req.setAllowDownload(false);
        req.setStatus("DRAFT");

        AuthenticatedUser loginUser = mockAdminUser();
        Long uploaderId = loginUser.id();
        String uploaderName = loginUser.nickname();

        Ppt result = pptService.createPpt(req, uploaderId, uploaderName);
        System.out.println("【新增成功】PPT ID：" + result.getId());
    }

    // 2. 分页查询PPT列表
    @Test
    void testPagePpt() {
        // 参数：keyword, status, uploadedFrom, uploadedTo, sortOrder, page, size
        IPage<PptListItem> pageResult = pptService.pagePpt(
                null,
                null,
                null,
                null,
                "desc",
                1,
                10
        );
        System.out.println("【分页查询】总条数：" + pageResult.getTotal());
        pageResult.getRecords().forEach(item ->
                System.out.println("ID:" + item.getId() + " | 标题:" + item.getTitle() + " | 状态:" + item.getStatus())
        );
    }

    // 3. 查询单条PPT详情
    @Test
    void testGetPptDetail() {
        Long targetPptId = 1L;
        PptDetailVO detail = pptService.getPptDetail(targetPptId);
        System.out.println("【PPT详情】");
        System.out.println("ID：" + detail.getId());
        System.out.println("标题：" + detail.getTitle());
        System.out.println("状态：" + detail.getStatus());
    }

    // 4. 编辑PPT基础信息
    @Test
    void testUpdatePpt() {
        Long targetPptId = 1L;
        UpdatePptRequest req = new UpdatePptRequest();
        req.setTitle("单元测试PPT-修改后标题");
        req.setDescription("修改后的简介内容");
        req.setAllowDownload(true);

        Ppt updateResult = pptService.updatePpt(targetPptId, req);
        System.out.println("【编辑成功】当前标题：" + updateResult.getTitle());
    }

    // 5. 修改PPT状态（草稿/发布/下架）
    @Test
    void testUpdateStatus() {
        Long targetPptId = 1L;
        // 可选 DRAFT / PUBLISHED / OFFLINE
        Ppt result = pptService.updateStatus(targetPptId, "PUBLISHED");
        System.out.println("【状态修改成功】当前状态：" + result.getStatus());
    }

    // 6. 获取PPT临时下载地址
    @Test
    void testGetDownloadUrl() {
        Long targetPptId = 1L;
        String downloadUrl = pptService.getDownloadUrl(targetPptId, 600);
        System.out.println("【下载链接】" + downloadUrl);
    }

    // 7. 删除PPT
    @Test
    void testDeletePpt() {
        Long targetPptId = 1L;
        pptService.deletePpt(targetPptId);
        System.out.println("【删除PPT执行完成】");
    }
}