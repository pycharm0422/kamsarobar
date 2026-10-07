package com.kamsarobar.media.dto;

import com.kamsarobar.media.Image;

/** {@code url} is relative to the API base, e.g. "/images/{id}". */
public record ImageResponse(String id, String url, Integer width, Integer height) {

    public static ImageResponse from(Image image) {
        return new ImageResponse(image.getId(), "/images/" + image.getId(), image.getWidth(), image.getHeight());
    }
}
