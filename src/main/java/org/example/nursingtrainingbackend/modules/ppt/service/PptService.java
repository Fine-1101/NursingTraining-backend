package org.example.nursingtrainingbackend.modules.ppt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.nursingtrainingbackend.modules.ppt.dto.CreatePptRequest;
import org.example.nursingtrainingbackend.modules.ppt.dto.UpdatePptRequest;
import org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt;
import org.example.nursingtrainingbackend.modules.ppt.vo.PptDetailVO;
import org.example.nursingtrainingbackend.modules.ppt.vo.PptListItem;

public interface PptService {

    Ppt createPpt(CreatePptRequest request, Long userId, String username);

    IPage<PptListItem> pagePpt(String keyword, String status, String uploadedFrom,
                               String uploadedTo, String sortOrder, Integer page, Integer size);

    PptDetailVO getPptDetail(Long id);

    Ppt updatePpt(Long id, UpdatePptRequest request);

    Ppt updateStatus(Long id, String status);

    String getDownloadUrl(Long id, Integer expiresIn);

    void deletePpt(Long id);
}
