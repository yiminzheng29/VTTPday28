package sg.edu.nus.iss.app.workshop28.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import sg.edu.nus.iss.app.workshop28.models.EditedComment;
import sg.edu.nus.iss.app.workshop28.models.Review;
import sg.edu.nus.iss.app.workshop28.services.ReviewService;

@RestController
@RequestMapping(path="/api/review")
public class ReviewRestController {
    
    @Autowired
    private ReviewService reviewSvc;

    // use postman to do putmapping
    // localhost:8080/api/review/63aac6992cb8117b3ebbc876

    @PutMapping(path = "{_id}")
    public ResponseEntity<String> updateEdits(@PathVariable String _id, @RequestBody EditedComment json) {
        JsonObject result = null;
        Review r = reviewSvc.updateEdits(_id, json);
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("review", r.toJson(false));
        result = builder.build();
        return ResponseEntity
            .status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(result.toString());
    }

    // http://localhost:8080/api/review/63aabffa4826f1449afdc125

    @GetMapping("{reviewId}")
    public ResponseEntity<String> getReviewsById(@PathVariable String reviewId) {
        JsonObject result = null;
        Review r = reviewSvc.getReview(reviewId);
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("review", r.toJson(true));
        result = builder.build();
        return ResponseEntity
            .status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(result.toString()); 
    }

    // http://localhost:8080/api/review/63aabffa4826f1449afdc125/history

    @GetMapping("{reviewId}/history")
    public ResponseEntity<String> getReviewHistory(@PathVariable String reviewId) {
        JsonObject result = null;
        Review r = reviewSvc.getReview(reviewId);
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("review", r.toJson(false));
        result = builder.build();
        return ResponseEntity
            .status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(result.toString());
    }
}
