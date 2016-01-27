/**
 * Defines Java Bean-style classes that implement the CloudKeeper model interfaces of
 * {@link com.svbio.cloudkeeper.model.bare}.
 *
 * This package defines Java Bean-style classes (also known as Plain Old Java Objects, or POJOs in short) of the
 * CloudKeeper domain-model interfaces. As such, all classes are mutable, serializable, and they have no-argument
 * constructors and setters. Each class has moreover a copy constructor accepting an interface from
 * {@link com.svbio.cloudkeeper.model.bare} as argument. These copy constructors perform a deep copy, and the
 * copy will share no mutable references with the original instance (this holds transitively). Whenever defensive copies
 * are necessary, for instance when these Bean-style classes are used in effectively immutable data structures, these
 * copy constructors should be used.
 *
 * All classes implement {@code equals()} in the canonical way, that is, by calling {@code equals()} on all fields.
 * Instances only compare equal to instances from this package (and <strong>not</strong> to other implementations of the
 * interfaces in package {@link com.svbio.cloudkeeper.model.bare}). Likewise, the classes in this package implement
 * {@code hashCode()} by recursively hashing the value of all their fields.
 *
 * As suggested in package {@link com.svbio.cloudkeeper.model.bare}, all classes in this package implement a
 * {@code toString()} method by delegating to the default implementation. The main use cases for this are debugging and
 * error messages.
 *
 * Setters in this package do <strong>not</strong> create defensive copies. For collection types, a shallow copy of all
 * elements is performed (for instance, using {@link java.util.Collection#addAll(java.util.Collection)}), whereas all
 * other properties are simply updated to the new value or reference. As a convenience, the setters in this package
 * return a reference to the the object they were called on, and therefore provide a "fluent" interface.
 *
 * Most properties of the classes in this package are created with the Java default values, namely {@code null} for most
 * properties. Correspondingly, getters may return {@code null} unless specifically noted, and setters accept
 * {@code null} as well. Obviously, this means that these classes may be used to model a logically inconsistent state.
 * Validation of the domain model is deferred to the CloudKeeper linker; only at that stage all information is present
 * that enables complete validation (for instance, referenced definitions are, in general, not available before link
 * time, and can therefore not be verified).
 *
 * All classes have proper annotations to use them with the Java Architecture for XML Binding (JAXB).
 */
@XmlSchema(
    namespace = JAXBConstants.NAMESPACE,
    location = JAXBConstants.LOCATION,
    elementFormDefault = XmlNsForm.QUALIFIED
)
@NonNullByDefault
package com.svbio.cloudkeeper.model.beans;

import com.svbio.cloudkeeper.model.util.JAXBConstants;
import com.svbio.cloudkeeper.model.util.NonNullByDefault;

import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
