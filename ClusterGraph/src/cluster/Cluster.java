package cluster;

import java.util.List;
import java.util.Objects;

public class Cluster {

    private Integer clusterNumber;
    private List<Integer> nodes;
    private String interval;

    public Cluster() {
    }

    public Cluster(Integer clusterNumber, List<Integer> nodes, String interval) {
        this.clusterNumber = clusterNumber;
        this.nodes = nodes;
        this.interval = interval;
    }

    public Integer getClusterNumber() {
        return clusterNumber;
    }

    public void setClusterNumber(Integer clusterNumber) {
        this.clusterNumber = clusterNumber;
    }

    public List<Integer> getNodes() {
        return nodes;
    }

    public void setNodes(List<Integer> nodes) {
        this.nodes = nodes;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.clusterNumber);
        hash = 97 * hash + Objects.hashCode(this.nodes);
        hash = 97 * hash + Objects.hashCode(this.interval);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Cluster other = (Cluster) obj;
        if (!Objects.equals(this.clusterNumber, other.clusterNumber)) {
            return false;
        }
        if (!Objects.equals(this.nodes, other.nodes)) {
            return false;
        }
        return Objects.equals(this.interval, other.interval);
    }

    @Override
    public String toString() {
        return getClusterNumber() + "\n" + getNodes();
    }

}
