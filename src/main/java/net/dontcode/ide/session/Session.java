package net.dontcode.ide.session;

import net.dontcode.core.Change;

import java.time.Instant;

public record Session (String id, Instant time, SessionActionType type, String srcInfo, Change change) {

}