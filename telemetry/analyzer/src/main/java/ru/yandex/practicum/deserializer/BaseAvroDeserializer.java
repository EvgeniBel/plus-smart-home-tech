package ru.yandex.practicum.deserializer;

import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public abstract class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private final DecoderFactory decoderFactory;
    private final Schema schema;

    public BaseAvroDeserializer(Schema schema) {
        this(DecoderFactory.get(), schema);
    }

    public BaseAvroDeserializer(DecoderFactory decoderFactory, Schema schema) {
        this.decoderFactory = decoderFactory;
        this.schema = schema;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            DatumReader<T> reader = new SpecificDatumReader<>(schema);
            Decoder decoder = decoderFactory.binaryDecoder(in, null);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new SerializationException("Ошибка десериализации данных из топика: " + topic, e);
        }
    }

    @Override
    public void close() {
        // Ничего не делаем
    }
}
