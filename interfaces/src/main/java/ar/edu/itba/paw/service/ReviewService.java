package ar.edu.itba.paw.service;

import java.util.UUID;

public interface ReviewService {
    double getAverageRating(UUID clubId);
}