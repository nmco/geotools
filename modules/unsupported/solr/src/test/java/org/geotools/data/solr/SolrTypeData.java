package org.geotools.data.solr;

import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/** Apache Solr Type data model, for load type definition from XML */
@XmlRootElement
public class SolrTypeData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String type;
    private Boolean multi;

    public SolrTypeData() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getMulti() {
        return multi;
    }

    public void setMulti(Boolean multi) {
        this.multi = multi;
    }

    @XmlRootElement
    public static class SolrTypes implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<SolrTypeData> types;

        public SolrTypes() {}

        public List<SolrTypeData> getTypes() {
            return types;
        }

        public void setTypes(List<SolrTypeData> types) {
            this.types = types;
        }
    }
}
