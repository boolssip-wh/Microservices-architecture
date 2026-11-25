UPDATE teacher_comment_profile SET tone = 'NEUTRAL' WHERE tone IN ('温和', '中性');
UPDATE teacher_comment_profile SET tone = 'ENCOURAGING' WHERE tone = '鼓励';
UPDATE teacher_comment_profile SET tone = 'STRICT' WHERE tone = '严格';
UPDATE teacher_comment_profile SET length_mode = 'MEDIUM' WHERE length_mode = '适中';
UPDATE teacher_comment_profile SET length_mode = 'SHORT' WHERE length_mode IN ('简短', '简洁');
UPDATE teacher_comment_profile SET length_mode = 'LONG' WHERE length_mode = '详细';
UPDATE teacher_comment_profile SET opening_style = 'PRAISE_FIRST' WHERE opening_style = '肯定优先';
UPDATE teacher_comment_profile SET opening_style = 'DIRECT' WHERE opening_style IN ('直接', '直接说明');
UPDATE teacher_comment_profile SET opening_style = 'QUESTION' WHERE opening_style = '问题引导';
