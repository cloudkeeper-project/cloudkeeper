package com.svbio.cloudkeeper.marshaling;

import com.svbio.cloudkeeper.model.api.MarshalContext;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.api.UnmarshalContext;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.util.ByteSequences;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class PersonMarshaler implements Marshaler<Person> {
    @Override
    public boolean isImmutable(Person object) {
        return false;
    }

    @Override
    public void put(Person person, MarshalContext context) throws IOException {
        context.writeObject(person.getAge(), SimpleName.identifier("age"));
        context.putByteSequence(
            ByteSequences.arrayBacked(person.getName().getBytes(StandardCharsets.UTF_8)),
            SimpleName.identifier("name")
        );
    }

    @Override
    public Person get(UnmarshalContext context) throws IOException {
        int age = (Integer) context.readObject(SimpleName.identifier("age"));
        String name = new String(
            context.getByteSequence(SimpleName.identifier("name")).toByteArray(),
            StandardCharsets.UTF_8
        );
        return new Person(age, name);
    }
}
