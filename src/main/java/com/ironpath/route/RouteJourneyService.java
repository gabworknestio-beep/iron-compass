package com.ironpath.route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Projects the flat canonical route into small, stable player-facing chapters. */
public final class RouteJourneyService
{
    public RouteJourney project(RouteProjection projection)
    {
        if (projection == null || projection.getRoute().getChapters().isEmpty())
        {
            return new RouteJourney(List.of(), null);
        }

        Map<String, Integer> stepIndexes = new HashMap<>();
        for (int index = 0; index < projection.getSteps().size(); index++)
        {
            stepIndexes.put(projection.getSteps().get(index).getStep().getId(), index);
        }

        List<RouteChapterProgress> chapters = new ArrayList<>();
        String currentStepId = projection.getCurrent() == null ? null : projection.getCurrent().getStep().getId();
        int currentStepIndex = currentStepId == null ? projection.getSteps().size()
            : stepIndexes.getOrDefault(currentStepId, projection.getSteps().size());
        int currentChapterIndex = -1;

        for (int chapterIndex = 0; chapterIndex < projection.getRoute().getChapters().size(); chapterIndex++)
        {
            RouteChapterSpec spec = projection.getRoute().getChapters().get(chapterIndex);
            int start = stepIndexes.get(spec.getStartStepId());
            int end = chapterIndex + 1 < projection.getRoute().getChapters().size()
                ? stepIndexes.get(projection.getRoute().getChapters().get(chapterIndex + 1).getStartStepId())
                : projection.getSteps().size();
            if (currentStepIndex >= start && currentStepIndex < end)
            {
                currentChapterIndex = chapterIndex;
            }
            List<StepEvaluation> steps = new ArrayList<>(projection.getSteps().subList(start, end));
            int complete = 0;
            int total = 0;
            for (StepEvaluation step : steps)
            {
                if (step.getStep().isOptional())
                {
                    continue;
                }
                total++;
                if (step.getStatus() == StepStatus.COMPLETE || step.getStatus() == StepStatus.SKIPPED_MANUALLY)
                {
                    complete++;
                }
            }
            chapters.add(new RouteChapterProgress(spec, steps, complete, total, chapterIndex == currentChapterIndex));
        }

        if (currentChapterIndex < 0 && !chapters.isEmpty())
        {
            currentChapterIndex = chapters.size() - 1;
            RouteChapterProgress last = chapters.get(currentChapterIndex);
            chapters.set(currentChapterIndex, new RouteChapterProgress(last.getChapter(), last.getSteps(),
                last.getCompleteCount(), last.getTotalCount(), true));
        }
        return new RouteJourney(chapters, currentChapterIndex < 0 ? null : chapters.get(currentChapterIndex));
    }
}
