package org.dontcode.ide.session;

import org.bson.BsonDocument;

import java.time.Instant;

public record Session (String id, Instant time, SessionActionType type, String srcInfo, BsonDocument doc) {

}