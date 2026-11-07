package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.training.entity.SrtpCustomField;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.mapper.SrtpCustomFieldMapper;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 自定义字段服务：按 (stage, batch) 汇总字段定义（全局默认 + 批次专属），
 * 并校验申报记录的 extra_data(JSON) 是否满足必填约束。四阶段共用。
 */
@Service
public class SrtpCustomFieldService {

    private final SrtpCustomFieldMapper customFieldMapper;
    private final JsonMapper objectMapper = JsonMapper.builder().build();

    public SrtpCustomFieldService(SrtpCustomFieldMapper customFieldMapper) {
        this.customFieldMapper = customFieldMapper;
    }

    /** 某阶段生效的字段定义：全局默认(batch_id=NULL) + 该批次专属，按 sort_order 升序 */
    public List<SrtpCustomField> definitions(SrtpStage stage, Long batchId) {
        List<SrtpCustomField> result = new ArrayList<>(customFieldMapper.selectList(
                new QueryWrapper<SrtpCustomField>().lambda()
                        .eq(SrtpCustomField::getStage, stage)
                        .isNull(SrtpCustomField::getBatchId)
                        .orderByAsc(SrtpCustomField::getSortOrder)));
        if (batchId != null) {
            result.addAll(customFieldMapper.selectList(
                    new QueryWrapper<SrtpCustomField>().lambda()
                            .eq(SrtpCustomField::getStage, stage)
                            .eq(SrtpCustomField::getBatchId, batchId)
                            .orderByAsc(SrtpCustomField::getSortOrder)));
        }
        return result;
    }

    /** 管理端：按 stage(+可选 batch) 列出字段定义用于配置 */
    public List<SrtpCustomField> listForAdmin(SrtpStage stage, Long batchId) {
        var w = new QueryWrapper<SrtpCustomField>().lambda().eq(SrtpCustomField::getStage, stage);
        if (batchId != null) w.eq(SrtpCustomField::getBatchId, batchId);
        else w.isNull(SrtpCustomField::getBatchId);
        w.orderByAsc(SrtpCustomField::getSortOrder);
        return customFieldMapper.selectList(w);
    }

    public SrtpCustomField save(SrtpCustomField f) {
        if (f.getFieldType() == null || f.getFieldType().isBlank()) f.setFieldType("TEXT");
        if (f.getId() == null) customFieldMapper.insert(f);
        else customFieldMapper.updateById(f);
        return f;
    }

    public void delete(Long id) {
        customFieldMapper.deleteById(id);
    }

    /** 校验自定义字段值(Map)满足必填约束 */
    public void validate(SrtpStage stage, Long batchId, Map<String, Object> data) {
        List<SrtpCustomField> defs = definitions(stage, batchId);
        Map<String, Object> safe = (data == null) ? Map.of() : data;
        for (SrtpCustomField def : defs) {
            if (Boolean.TRUE.equals(def.getRequired())) {
                Object v = safe.get(def.getFieldKey());
                if (v == null || (v instanceof String s && s.isBlank())) {
                    throw new BusinessException("请填写必填项：" + def.getLabel());
                }
            }
        }
    }

    /** 将自定义字段值(Map)序列化为 JSON 文本，用于存入 extra_data 列 */
    public String toJson(Map<String, Object> data) {
        if (data == null || data.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new BusinessException("自定义字段数据格式错误");
        }
    }

    /** 校验 extra_data(JSON) 满足必填约束；解析失败或缺必填抛 BusinessException */
    public void validate(SrtpStage stage, Long batchId, String extraDataJson) {
        List<SrtpCustomField> defs = definitions(stage, batchId);
        boolean anyRequired = defs.stream().anyMatch(d -> Boolean.TRUE.equals(d.getRequired()));
        if (defs.isEmpty() || !anyRequired) return;

        Map<String, Object> data = parse(extraDataJson);
        for (SrtpCustomField def : defs) {
            if (Boolean.TRUE.equals(def.getRequired())) {
                Object v = data.get(def.getFieldKey());
                if (v == null || (v instanceof String s && s.isBlank())) {
                    throw new BusinessException("请填写必填项：" + def.getLabel());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new BusinessException("自定义字段数据格式错误");
        }
    }
}
