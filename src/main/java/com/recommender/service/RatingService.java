package com.recommender.service;


import com.recommender.entity.MovieEntity;
import com.recommender.entity.RatingEntity;
import com.recommender.entity.UserEntity;
import com.recommender.kafka.AvroProducerService;
import com.recommender.model.Rating;
import com.recommender.model.RatingPut;
import com.recommender.repository.MovieRepository;
import com.recommender.repository.RatingRepository;
import com.recommender.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final AvroProducerService avroProducerService;

    public void deleteRating(Long userId, Long movieId) {
        ratingRepository.findByUserIdAndMovieId(userId, movieId).ifPresent(ratingRepository::delete);
    }

    public Rating updateRating(Long userId, Long movieId, RatingPut rating) {
        return ratingRepository.findByUserIdAndMovieId(userId, movieId)
                .map(ratingEntity -> {
                    ratingEntity.setRating(rating.getRating());
                    Rating saved =  ratingRepository.save(ratingEntity).toRating();
                    avroProducerService.sendRatingEvent(movieId, userId, rating.getRating());
                    return saved;
                })
                .orElse(null);
    }

    public Rating createRating(Rating rating) {
        if (ratingRepository.existsByUserIdAndMovieId(rating.getUserId(), rating.getMovieId())) {
            throw new IllegalArgumentException("Rating already exists");
        }
        Optional<UserEntity> user = userRepository.findById(rating.getUserId());
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        Optional<MovieEntity> movies = movieRepository.findById(rating.getMovieId());
        if (movies.isEmpty()) {
            throw new IllegalArgumentException("Movie not found");
        }

        RatingEntity ratingEntity = new RatingEntity();
        ratingEntity.setRating(rating.getRating());
        ratingEntity.setUser(user.get());
        ratingEntity.setMovie(movies.get());
        Rating saved = ratingRepository.save(ratingEntity).toRating();
        avroProducerService.sendRatingEvent(rating.getMovieId(), rating.getUserId(), rating.getRating());
        return saved;
    }
}
