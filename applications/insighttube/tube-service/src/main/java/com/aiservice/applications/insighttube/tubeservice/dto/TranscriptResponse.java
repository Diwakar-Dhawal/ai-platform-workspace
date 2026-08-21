package com.aiservice.applications.insighttube.tubeservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class TranscriptResponse {

    private String status;
    private String url;
    private String contentType;
    private List<VideoTranscript> videos;
    private int totalVideos;
    private int totalSegments;
    private boolean cached;

    @Data
    public static class VideoTranscript {
        private String videoId;
        private String title;
        private String channelName;
        private double durationSeconds;
        private String language;
        private List<TranscriptSegment> segments;
        private String fullText;
        private int segmentCount;
    }

    @Data
    public static class TranscriptSegment {
        private String text;
        private double start;
        private double duration;
    }
}
