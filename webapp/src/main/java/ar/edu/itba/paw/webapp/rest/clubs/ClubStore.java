package ar.edu.itba.paw.webapp.rest.clubs;

import ar.edu.itba.paw.model.dto.ClubDto;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClubStore {
    private ClubStore() {}

    public static final Map<UUID, ClubDto> CLUBS = new ConcurrentHashMap<>();
}