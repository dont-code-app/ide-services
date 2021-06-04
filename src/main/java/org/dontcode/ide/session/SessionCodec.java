package org.dontcode.ide.session;

import com.mongodb.MongoClientSettings;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.conversions.Bson;

import java.time.ZoneId;
import java.util.UUID;

public class SessionCodec implements CollectibleCodec<Session> {
    private final Codec<Document> documentCodec;

    public SessionCodec() {
        this.documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public void encode(BsonWriter writer, Session session, EncoderContext encoderContext) {
        Document doc = new Document();
        doc.put("id", session.id());
        if( session.time() != null)
            doc.put("time", session.time().atZone(ZoneId.systemDefault()).toLocalDateTime());
        if( session.type() != null)
            doc.put("type", session.type().name());
        if( session.srcInfo() != null)
            doc.put("srcInfo", session.srcInfo());
        if( session.doc()!=null)
            doc.put("change", session.doc());
        documentCodec.encode(writer, doc, encoderContext);
    }

    @Override
    public Class<Session> getEncoderClass() {
        return Session.class;
    }

    @Override
    public Session generateIdIfAbsentFromDocument(Session document) {
        if (!documentHasId(document)) {
            document = new Session (UUID.randomUUID().toString(),document.time(), document.type(), document.srcInfo(), document.doc());
        }
        return document;
    }

    @Override
    public boolean documentHasId(Session document) {
        return document.id() != null;
    }

    @Override
    public BsonValue getDocumentId(Session document) {
        return new BsonString(document.id());
    }

    @Override
    public Session decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(reader, decoderContext);
        var changeDoc =document.get("change", Document.class);
        Session session = new Session(document.getString("id"),
                document.getDate("time").toInstant(),
                SessionActionType.valueOf(document.getString("type")),
                document.getString("srcInfo"),
                (changeDoc==null)?null:changeDoc.toBsonDocument());
        return session;
    }
}
