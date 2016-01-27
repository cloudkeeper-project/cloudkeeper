@XmlSchema(
    namespace = JAXBConstants.NAMESPACE,
    location = JAXBConstants.LOCATION,
    elementFormDefault = XmlNsForm.QUALIFIED
)
@XmlJavaTypeAdapters(
    @XmlJavaTypeAdapter(value = JAXBAdapters.PrimitiveKindAdapter.class, type = BarePrimitiveType.Kind.class)
)
@NonNullByDefault
package com.svbio.cloudkeeper.model.beans.type;

import com.svbio.cloudkeeper.model.bare.type.BarePrimitiveType;
import com.svbio.cloudkeeper.model.util.JAXBConstants;
import com.svbio.cloudkeeper.model.util.NonNullByDefault;

import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
