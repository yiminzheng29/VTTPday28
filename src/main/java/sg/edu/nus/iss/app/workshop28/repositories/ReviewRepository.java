package sg.edu.nus.iss.app.workshop28.repositories;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;


import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation; // to add this
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation.AddFieldsOperationBuilder;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import sg.edu.nus.iss.app.workshop28.models.Comment;
import sg.edu.nus.iss.app.workshop28.models.EditedComment;
import sg.edu.nus.iss.app.workshop28.models.Game;
import sg.edu.nus.iss.app.workshop28.models.Review;

@Repository
public class ReviewRepository {
    
    public static final String REVIEWS_COL = "reviews";

    @Autowired
    private MongoTemplate mongoTemplate;

    public Review insertReview(Review r) {
        return mongoTemplate.insert(r, REVIEWS_COL);
    }

    public Review getReview(String _id) {
        ObjectId docId = new ObjectId(_id);
        return mongoTemplate.findById(docId, Review.class, REVIEWS_COL);
    }

    public Review updateEdits(String _id, EditedComment c) {
        ObjectId docId = new ObjectId(_id);
        Review r = mongoTemplate.findById(docId, Review.class, REVIEWS_COL);
        if (r!=null) {
            EditedComment e = new EditedComment();
            e.setComment(c.getComment());
            e.setRating(c.getRating());
            e.setPosted(LocalDateTime.now());
            if (r.getEdited()!=null) {
                r.getEdited().add(e);
            } else {
                List<EditedComment> ll = new LinkedList<EditedComment>();
                ll.add(e);
                r.setEdited(ll);
            }
            mongoTemplate.save(r, REVIEWS_COL);
        }
        return r;
    }

    /**
     * db.game.aggregate([ { $match: { gid: 3 } }, { $lookup: { from: "reviews",
     * localField: "gid", foreignField: "gameId", as: "reviewsDocs" } }, { $project:
     * { _id: 1, gid: 1, name: 1, year: 1, ranking: 1, users_rated: 1, url: 1,
     * image: 1, reviews: "$reviewsDocs._id", timestamp: "$$NOW" } }]);
     */

    public Optional<Game> aggregateGameReviews(String gameId) {
        // To create a match aggregate
        MatchOperation matchGameId = Aggregation.match(
            Criteria.where("gid").is(Integer.parseInt(gameId))
        );

        // Joins the collections based on gid and gameId
        LookupOperation linkReviewsGame = Aggregation.lookup("comment", "gid", "gid", "reviewsDocs"); 
        // retrieves comments from the comment collection
        // localfield gid is from comment collection
        // foreignField gid is from game collection

        // Projection can be used for suppressing fields / adding new fields. Commonly used to reduce result size and transform structure of document
        ProjectionOperation projection = Aggregation.project("_id", "gid", "name", "year", "ranking", "users_rated", "url", "image")
            .and("reviewsDocs").as("reviews"); // reviews word to match with the Game.java under create

        AddFieldsOperationBuilder addFieldOpsBld = Aggregation.addFields();
        addFieldOpsBld.addFieldWithValue("timestamp", LocalDateTime.now());
        AddFieldsOperation newFieldOps = addFieldOpsBld.build();

        // pipeline is a series of document transformers and each transformer will receive a stream of documents (either directly from the collection or from an ealier transformer)
        // transformer process the document and outputs the transformed document
        Aggregation pipeline = Aggregation.newAggregation(matchGameId, linkReviewsGame, projection, newFieldOps);
        
        // Perform the aggregation on the collection with the defined pipleline. Results are returned as document
        AggregationResults<Document> results = mongoTemplate.aggregate(pipeline, "game", Document.class); // retrieves results from game collections

        Document doc = results.iterator().next();
        Game g = Game.create(doc);
        return Optional.of(g);

        
        }

        public List<Comment> aggregateGamesComment(Integer limit, String username, Integer rating) {
            Criteria andCriteria = null;
            if (rating>5) {
                andCriteria = new Criteria().andOperator(
                    Criteria.where("user").is(username),
                    Criteria.where("rating").gt(rating)); // gt = greater than
            } else {
                andCriteria = new Criteria().andOperator(
                    Criteria.where("user").is(username),
                    Criteria.where("rating").lt(rating)); // lt = less than
            }
            MatchOperation matchUsernameOp = Aggregation.match(andCriteria);

            LookupOperation linkReviewsGame = Aggregation.lookup("game", "gid", "gid", "gameComment");

            ProjectionOperation projection = Aggregation.project("_id", "c_id", "user", "rating", "c_text", "gid", "name") // name of game is now saved as game_name
                .and("gameComment.name").as("game_name");

            LimitOperation limitRecords = Aggregation.limit(limit);

            Aggregation pipeline = Aggregation.newAggregation(matchUsernameOp, linkReviewsGame, limitRecords, projection);
            AggregationResults<Document> results = mongoTemplate.aggregate(pipeline, "comment", Document.class); // changed from Comment.class to Document.class as it is necessary to reflect the name
            // retrieves from comment collection
            List<Document> docs = results.getMappedResults();
            List<Comment> comments = new LinkedList<>();
            for (Document d: docs) {
                comments.add(Comment.create(d));
            }
            return comments;
        } 

        // for adding date as a query based on ISO date
        public Optional<Game> getGamesByCommentDate(String gid, String date) {

            String str = date.concat(" 00:00");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
    
            Criteria c = Criteria.where("posted").gt(dateTime);
    
            Query query = Query.query(c);
    
            List<Document> docs = mongoTemplate.find(query, Document.class, "reviews");
            System.out.println(docs.toString());
    
            MatchOperation matchGameId = Aggregation.match(Criteria.where("gid").is(Integer.parseInt(gid)));
    
            LookupOperation linkReviewsGame = Aggregation.lookup("reviews",
                    "gid", "gameId", "reviewsDocs");
    
            ProjectionOperation projection = Aggregation
                    .project("_id", "gid", "name", "year", "ranking",
                            "users_rated", "url", "image")
                    .and("reviewsDocs").as("reviews");
    
            AddFieldsOperationBuilder addFieldOpsBld = Aggregation.addFields();
            addFieldOpsBld.addFieldWithValue("timestamp", LocalDateTime.now());
            addFieldOpsBld.addField(docs.toString());
            AddFieldsOperation newFieldOps = addFieldOpsBld.build();
            
    
            Aggregation pipeline = Aggregation
                    .newAggregation(matchGameId, linkReviewsGame, projection, newFieldOps);
            AggregationResults<Document> results = mongoTemplate
                    .aggregate(pipeline, "game", Document.class);
            if (!results.iterator().hasNext())
                return Optional.empty();
    
            Document doc = results.iterator().next();
            Game g = Game.create(doc);
            return Optional.of(g);
    
        }
    }

    
