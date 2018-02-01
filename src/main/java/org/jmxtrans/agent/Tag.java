package org.jmxtrans.agent;

import org.jmxtrans.agent.util.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Tag {
    private final String name;
    private final String value;
    private final String separator;

    public Tag(String name, String value) {
        this.name = Objects.requireNonNull(name);
        this.value = Objects.requireNonNull(value);
        this.separator = ":";
    }

    public Tag(String name, String value, String separator) {
        this.name = Objects.requireNonNull(name);
        this.value = Objects.requireNonNull(value);
        this.separator = Objects.requireNonNull(separator);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getSeparator() { return separator; }

    public String toTagFormat() {
        return name + separator + value;
    }

    @Override
    public String toString() {
        return name + separator + value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tag other = (Tag) obj;
        return Objects.equals(name, other.name)
                && Objects.equals(value, other.value);
    }

    public static List<Tag> tagsFromCommaSeparatedString(String s) {
        List<Tag> tags = new ArrayList<>();
        if (s.trim().isEmpty()) {
            return tags;
        }
        String[] parts = s.split(",");
        for (String tagPart : parts) {
            tags.add(parseOneTag(tagPart));
        }
        return tags;
    }

    public static List<Tag> tagsFromCommaSeparatedString(String s, String separator) {
        List<Tag> tags = new ArrayList<>();
        if (s.trim().isEmpty()) {
            return tags;
        }
        String[] parts = s.split(",");
        for (String tagPart : parts) {
            tags.add(parseOneTag(tagPart, separator));
        }
        return tags;
    }

    private static Tag parseOneTag(String part) {
        String[] nameAndValue = part.trim().split(":");
        if (nameAndValue.length != 2) {
            throw new RuntimeException(
                    "Error when parsing tags from substring " + part + ", must be on format <name>:<value>,...");
        }
        Tag tag = new Tag(nameAndValue[0].trim(), getValueProperties(nameAndValue[1].trim()));
        return tag;
    }

    private static Tag parseOneTag(String part, String separator) {
        String[] nameAndValue = part.trim().split(separator);
        if (nameAndValue.length != 2) {
            throw new RuntimeException(
                    "Error when parsing influx from substring " + part + ", must be on format <name"+separator+"<value>,...");
        }
        Tag tag = new Tag(nameAndValue[0].trim(),  getValueProperties(nameAndValue[1].trim()), separator);
        return tag;
    }

    public static List<String> convertTagsToStrings(List<Tag> tags) {
        List<String> l = new ArrayList<>(tags.size());
        for (Tag taglist : tags) {
            l.add(taglist.toTagFormat());
        }
        return l;
    }

    private static String getValueProperties(String value) {
        String env = System.getenv(value);
        if (env == null) {
            return value;
        } else {
            return env;
        }

    }
}
