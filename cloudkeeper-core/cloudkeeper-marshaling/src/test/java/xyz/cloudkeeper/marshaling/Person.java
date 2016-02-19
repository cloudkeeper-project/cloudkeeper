package xyz.cloudkeeper.marshaling;

import javax.annotation.Nullable;

final class Person {
    private final int age;
    private final String name;

    Person(int age, String name) {
        this.age = age;
        this.name = name;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        Person other = (Person) otherObject;
        return age == other.age
            && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        int result = age;
        result = 31 * result + name.hashCode();
        return result;
    }

    public int getAge() {
        return age;
    }

    public String getName() {
        return name;
    }
}
