package model.codecs;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.Instant;

public class InstantCodec implements Codec<Instant> {
	@Override
	public Instant decode(BsonReader reader, DecoderContext decoderContext) {
		return Instant.ofEpochSecond(reader.readDateTime());
	}
	
	@Override
	public void encode(BsonWriter writer, Instant value, EncoderContext encoderContext) {
		writer.writeDateTime(value.getEpochSecond());
	}
	
	@Override
	public Class<Instant> getEncoderClass() {
		return Instant.class;
	}
}
