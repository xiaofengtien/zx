package com.zx.student.archive.domain.dto.paper;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * Python 解析服务返回的结果 DTO
 *
 * @author zx
 */
@Data
public class ParseResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否解析成功
     */
    private Boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 试卷名称
     */
    private String paperName;

    /**
     * 试卷类型（gaokao/zhongkao等）
     */
    private PaperTypeDTO paperType;

    /**
     * 题目总数
     */
    private Integer questionCount;

    /**
     * 卷别列表
     */
    private List<ParsedVolumeDTO> volumes;

    /**
     * 所有题目列表（扁平化）
     */
    private List<ParsedQuestionDTO> questions;

    /**
     * 音频切分结果
     */
    private List<AudioChunkDTO> audioChunks;

    /**
     * 警告信息
     */
    private List<String> warnings;

    /**
     * 试卷类型 DTO
     */
    @Data
    public static class PaperTypeDTO implements Serializable {
        private String type;
        private String typeName;
        private String confidence;
    }

    /**
     * 解析的卷别 DTO
     */
    @Data
    public static class ParsedVolumeDTO implements Serializable {
        private String code;
        private String name;
        private Double totalScore;
        private List<ParsedSectionDTO> sections;

        @com.fasterxml.jackson.annotation.JsonProperty("is_listening")
        @com.fasterxml.jackson.annotation.JsonAlias("isListening")
        private Boolean isListening;

        /**
         * 卷别音频URL（从 test_audio 填充）
         */
        /**
         * 卷别音频URL（从 volume_intro 填充）
         */
        @com.fasterxml.jackson.annotation.JsonProperty("volume_audio_url")
        @com.fasterxml.jackson.annotation.JsonAlias({ "volumeAudioUrl", "audioUrl", "audio_url" })
        private String audioUrl;

        @com.fasterxml.jackson.annotation.JsonProperty("volume_audio_path")
        @com.fasterxml.jackson.annotation.JsonAlias("volumeAudioPath")
        private String volumeAudioPath;

        @com.fasterxml.jackson.annotation.JsonProperty("volume_audio_duration")
        @com.fasterxml.jackson.annotation.JsonAlias("volumeAudioDuration")
        private Double volumeAudioDuration;

        public void setAudioUrl(String audioUrl) {
            this.audioUrl = audioUrl;
        }

        public String getAudioUrl() {
            return audioUrl;
        }

        public void setVolumeAudioPath(String volumeAudioPath) {
            this.volumeAudioPath = volumeAudioPath;
        }

        public String getVolumeAudioPath() {
            return volumeAudioPath;
        }

        public void setVolumeAudioDuration(Double volumeAudioDuration) {
            this.volumeAudioDuration = volumeAudioDuration;
        }

        public Double getVolumeAudioDuration() {
            return volumeAudioDuration;
        }
    }

    /**
     * 解析的大题 DTO
     */
    @Data
    public static class ParsedSectionDTO implements Serializable {
        private String name;
        private String instruction;
        @com.fasterxml.jackson.annotation.JsonProperty("score_per_question")
        @com.fasterxml.jackson.annotation.JsonAlias("scorePerQuestion")
        private Double scorePerQuestion;
        private List<ParsedQuestionDTO> questions;
        @com.fasterxml.jackson.annotation.JsonProperty("question_groups")
        @com.fasterxml.jackson.annotation.JsonAlias("questionGroups")
        private List<ParsedQuestionGroupDTO> questionGroups;

        /**
         * 大题说明音频URL（从 section_intro 填充）
         */
        @com.fasterxml.jackson.annotation.JsonProperty("intro_audio_url")
        @com.fasterxml.jackson.annotation.JsonAlias("introAudioUrl")
        private String introAudioUrl;

        @com.fasterxml.jackson.annotation.JsonProperty("intro_audio_path")
        @com.fasterxml.jackson.annotation.JsonAlias("introAudioPath")
        private String introAudioPath;

        @com.fasterxml.jackson.annotation.JsonProperty("intro_audio_duration")
        @com.fasterxml.jackson.annotation.JsonAlias("introAudioDuration")
        private Double introAudioDuration;

        public void setIntroAudioUrl(String introAudioUrl) {
            this.introAudioUrl = introAudioUrl;
        }

        public String getIntroAudioUrl() {
            return introAudioUrl;
        }

        public void setIntroAudioPath(String introAudioPath) {
            this.introAudioPath = introAudioPath;
        }

        public String getIntroAudioPath() {
            return introAudioPath;
        }

        public void setIntroAudioDuration(Double introAudioDuration) {
            this.introAudioDuration = introAudioDuration;
        }

        public Double getIntroAudioDuration() {
            return introAudioDuration;
        }
    }

    /**
     * 解析的题目组 DTO（听力语篇/阅读语篇）
     */
    @Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParsedQuestionGroupDTO implements Serializable {
        @com.fasterxml.jackson.annotation.JsonProperty("intro_text")
        @com.fasterxml.jackson.annotation.JsonAlias("introText")
        private String introText;

        @com.fasterxml.jackson.annotation.JsonProperty("has_audio")
        @com.fasterxml.jackson.annotation.JsonAlias("hasAudio")
        private Boolean hasAudio;

        @com.fasterxml.jackson.annotation.JsonProperty("start_index")
        @com.fasterxml.jackson.annotation.JsonAlias("startIndex")
        private Integer startIndex;

        @com.fasterxml.jackson.annotation.JsonProperty("end_index")
        @com.fasterxml.jackson.annotation.JsonAlias("endIndex")
        private Integer endIndex;

        /**
         * 组音频URL（从 question_group_audio_map 填充）
         */
        private String audioUrl;

        /**
         * 组音频路径（从 question_group_audio_map 填充）
         */
        private String audioPath;

        /**
         * 组音频时长（秒）
         */
        private Double audioDuration;

        /**
         * 题目组标题
         */
        private String title;

        /**
         * 创建的题目组ID（导入后填充）
         */
        private Integer questionGroupId;

        @com.fasterxml.jackson.annotation.JsonProperty("group_name")
        @com.fasterxml.jackson.annotation.JsonAlias("groupName")
        private String groupName;

        @com.fasterxml.jackson.annotation.JsonProperty("answer_time")
        @com.fasterxml.jackson.annotation.JsonAlias("answerTime")
        private Integer answerTime;

        private List<ParsedQuestionDTO> questions;
    }

    /**
     * 解析的题目 DTO
     */
    @Data
    public static class ParsedQuestionDTO implements Serializable {
        private Integer index;
        private String title;
        private Integer type;
        private String answer;
        private String analysis;
        private String originalText;
        private Boolean hasAudio;
        private String wordLimit;
        private List<String> requirements;
        private List<ParsedOptionDTO> options;
        private AudioChunkDTO audioChunk;

        /**
         * 创建后的题目ID（批量创建题目后填充）
         */
        private Integer questionId;

        /**
         * 单题音频URL（从 single_question_audio_map 填充）
         */
        private String audioUrl;

        /**
         * 单题音频时长（秒）
         */
        private Double audioDuration;
    }

    /**
     * 解析的选项 DTO
     */
    @Data
    public static class ParsedOptionDTO implements Serializable {
        private String label;
        private String content;
        @com.fasterxml.jackson.annotation.JsonProperty("is_answer")
        @com.fasterxml.jackson.annotation.JsonAlias("isAnswer")
        private Boolean isAnswer;
    }

    /**
     * 音频片段 DTO
     */
    @Data
    public static class AudioChunkDTO implements Serializable {
        private Integer index;
        private Double startTime;
        private Double endTime;
        private Double duration;
        private String filePath;
        private String base64Data; // base64 编码的音频数据
        private String fileName; // 文件名
        private String ossUrl; // OSS URL（上传后填充）
    }
}
