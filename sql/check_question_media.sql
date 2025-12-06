-- ============================================
-- 检查题目音频数据
-- ============================================

-- 1. 检查 question_media 表中是否有题目音频数据（media_type=1 或 4）
SELECT 
    qm.id,
    qm.question_id,
    qm.media_type,
    qm.media_name,
    qm.media_url,
    qm.media_path,
    qm.option_id,
    q.title AS question_title
FROM question_media qm
LEFT JOIN question q ON qm.question_id = q.id
WHERE qm.media_type IN (1, 4)
  AND qm.option_id IS NULL
ORDER BY qm.question_id, qm.media_type;

-- 2. 统计各种 media_type 的数量
SELECT 
    media_type,
    COUNT(*) AS count,
    CASE media_type
        WHEN 1 THEN '题目媒体（旧）'
        WHEN 2 THEN '选项媒体'
        WHEN 3 THEN '辅助识图'
        WHEN 4 THEN '题目音频'
        WHEN 5 THEN '讲解音频'
        WHEN 6 THEN '讲解图片'
        ELSE '其他'
    END AS type_name
FROM question_media
GROUP BY media_type
ORDER BY media_type;

-- 3. 检查是否有 media_type=1 且 option_id IS NULL 的记录（需要转换为 media_type=4）
SELECT 
    COUNT(*) AS need_convert_count
FROM question_media
WHERE media_type = 1 
  AND option_id IS NULL;

-- 4. 检查具体的题目（以 PAPER_20251120_001 为例）
SELECT DISTINCT
    pq.paper_id,
    pq.question_id,
    qm.media_type,
    qm.media_name,
    qm.option_id
FROM paper_question pq
LEFT JOIN question_media qm ON pq.question_id = qm.question_id
WHERE pq.paper_id = (SELECT id FROM paper WHERE paper_code = 'PAPER_20251120_001')
  AND qm.media_type IN (1, 4)
  AND qm.option_id IS NULL
ORDER BY pq.question_id;


