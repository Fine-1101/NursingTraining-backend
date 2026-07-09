package org.example.nursingtrainingbackend.modules.tag.service;

import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.tag.dto.TagBatchDTO;
import org.example.nursingtrainingbackend.modules.tag.dto.TagCreateDTO;
import org.example.nursingtrainingbackend.modules.tag.dto.TagQueryDTO;
import org.example.nursingtrainingbackend.modules.tag.dto.TagStatusDTO;
import org.example.nursingtrainingbackend.modules.tag.dto.TagUpdateDTO;
import org.example.nursingtrainingbackend.modules.tag.vo.TagBatchResultVO;
import org.example.nursingtrainingbackend.modules.tag.vo.TagItemVO;
import org.example.nursingtrainingbackend.modules.tag.vo.TagOverviewVO;
import org.example.nursingtrainingbackend.modules.tag.vo.TagStatisticsVO;
import org.example.nursingtrainingbackend.modules.tag.vo.TagStatusVO;

public interface TagService {

    PageResult<TagItemVO> queryPage(TagQueryDTO query);

    TagItemVO getDetail(Long id);

    TagItemVO createTag(TagCreateDTO dto);

    TagItemVO updateTag(Long id, TagUpdateDTO dto);

    TagStatusVO updateStatus(Long id, TagStatusDTO dto);

    void deleteTag(Long id);

    TagBatchResultVO batchOperate(TagBatchDTO dto);

    TagOverviewVO getOverview();

    TagStatisticsVO getStatistics();
}
