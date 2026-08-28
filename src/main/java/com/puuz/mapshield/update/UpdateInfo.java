package com.puuz.mapshield.update;

/** Immutable update information returned by the background checker. */
public record UpdateInfo(String version, String releaseUrl) {
}
