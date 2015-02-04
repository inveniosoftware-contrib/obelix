package graph;

import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

public class TimeStampFilterEvaluator implements Evaluator {

    String sinceTimestamp;
    String untilTimestmap;

    public TimeStampFilterEvaluator(String sinceTimestamp, String untilTimestmap) {
        this.sinceTimestamp = sinceTimestamp;
        this.untilTimestmap = untilTimestmap;
    }

    @Override
    public Evaluation evaluate(Path path) {

        if (path.lastRelationship() != null) {

            if (sinceTimestamp != null && untilTimestmap != null) {

                Long until = Long.parseLong(untilTimestmap);
                Long since = Long.parseLong(sinceTimestamp);
                Long current = Long.parseLong(path.lastRelationship().getProperty("timestamp").toString());

                if (current > until || current < since) {
                    return Evaluation.of(false, false);
                } else {
                    return Evaluation.ofIncludes(true);
                }

            } else if (sinceTimestamp != null) {

                Long since = Long.parseLong(sinceTimestamp);
                Long current = Long.parseLong(path.lastRelationship().getProperty("timestamp").toString());
                if (current < since) {
                    return Evaluation.of(false, false);
                } else {
                    return Evaluation.ofIncludes(true);
                }

            } else if (untilTimestmap != null) {
                Long until = Long.parseLong(untilTimestmap);
                Long current = Long.parseLong(path.lastRelationship().getProperty("timestamp").toString());
                if (current > until) {
                    return Evaluation.of(false, false);
                } else {
                    return Evaluation.ofIncludes(true);
                }
            }

        }

        return Evaluation.ofIncludes(true);

    }
}