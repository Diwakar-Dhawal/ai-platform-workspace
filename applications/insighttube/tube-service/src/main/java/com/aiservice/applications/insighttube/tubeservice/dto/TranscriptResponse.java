package com.aiservice.applications.insighttube.tubeservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranscriptResponse {

    private String status;
    private String url;

    @JsonProperty("content_type")
    private String contentType;

    private List<VideoTranscript> videos;

    @JsonProperty("total_videos")
    private int totalVideos;

    @JsonProperty("total_segments")
    private int totalSegments;

    private boolean cached;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoTranscript {

        @JsonProperty("video_id")
        private String videoId;

        private String title;

        @JsonProperty("channel_name")
        private String channelName;

        @JsonProperty("duration_seconds")
        private double durationSeconds;

        private String language;
        private List<TranscriptSegment> segments;

        @JsonProperty("full_text")
        private String fullText;

        @JsonProperty("segment_count")
        private int segmentCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TranscriptSegment {
        private String text;
        private double start;
        private double duration;
    }
}
