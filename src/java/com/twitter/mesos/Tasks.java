package com.twitter.mesos;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Ordering;

import com.twitter.mesos.gen.AssignedTask;
import com.twitter.mesos.gen.Identity;
import com.twitter.mesos.gen.JobConfiguration;
import com.twitter.mesos.gen.ScheduleStatus;
import com.twitter.mesos.gen.ScheduledTask;
import com.twitter.mesos.gen.TaskEvent;
import com.twitter.mesos.gen.TwitterTaskInfo;

import static com.twitter.mesos.gen.ScheduleStatus.ASSIGNED;
import static com.twitter.mesos.gen.ScheduleStatus.FAILED;
import static com.twitter.mesos.gen.ScheduleStatus.FINISHED;
import static com.twitter.mesos.gen.ScheduleStatus.KILLED;
import static com.twitter.mesos.gen.ScheduleStatus.KILLING;
import static com.twitter.mesos.gen.ScheduleStatus.LOST;
import static com.twitter.mesos.gen.ScheduleStatus.PENDING;
import static com.twitter.mesos.gen.ScheduleStatus.PREEMPTING;
import static com.twitter.mesos.gen.ScheduleStatus.RESTARTING;
import static com.twitter.mesos.gen.ScheduleStatus.ROLLBACK;
import static com.twitter.mesos.gen.ScheduleStatus.RUNNING;
import static com.twitter.mesos.gen.ScheduleStatus.STARTING;
import static com.twitter.mesos.gen.ScheduleStatus.UPDATING;

/**
 * Utility class providing convenience functions relating to tasks.
 *
 * @author William Farner
 */
public class Tasks {

  public static final Function<ScheduledTask, AssignedTask> SCHEDULED_TO_ASSIGNED =
      new Function<ScheduledTask, AssignedTask>() {
        @Override public AssignedTask apply(ScheduledTask task) {
          return task.getAssignedTask();
        }
      };

  public static final Function<AssignedTask, TwitterTaskInfo> ASSIGNED_TO_INFO =
      new Function<AssignedTask, TwitterTaskInfo>() {
        @Override public TwitterTaskInfo apply(AssignedTask task) {
          return task.getTask();
        }
      };

  public static final Function<ScheduledTask, TwitterTaskInfo> SCHEDULED_TO_INFO =
      Functions.compose(ASSIGNED_TO_INFO, SCHEDULED_TO_ASSIGNED);

  public static final Function<AssignedTask, String> ASSIGNED_TO_ID =
      new Function<AssignedTask, String>() {
        @Override public String apply(AssignedTask task) {
          return task.getTaskId();
        }
      };

  public static final Function<ScheduledTask, String> SCHEDULED_TO_ID =
      Functions.compose(ASSIGNED_TO_ID, SCHEDULED_TO_ASSIGNED);

  public static final Function<TwitterTaskInfo, Integer> INFO_TO_SHARD_ID =
      new Function<TwitterTaskInfo, Integer>() {
        @Override public Integer apply(TwitterTaskInfo task) {
          return task.getShardId();
        }
      };

  public static final Function<ScheduledTask, Integer> SCHEDULED_TO_SHARD_ID =
      Functions.compose(INFO_TO_SHARD_ID, SCHEDULED_TO_INFO);

  public static final Function<ScheduledTask, Iterable<TaskEvent>> GET_TASK_EVENTS =
      new Function<ScheduledTask, Iterable<TaskEvent>>() {
        @Override public Iterable<TaskEvent> apply(ScheduledTask task) {
          return task.getTaskEvents();
        }
      };

  public static final Function<TwitterTaskInfo, String> INFO_TO_JOB_KEY =
      new Function<TwitterTaskInfo, String>() {
        @Override public String apply(TwitterTaskInfo info) {
          return jobKey(info);
        }
      };

  public static final Function<AssignedTask, String> ASSIGNED_TO_JOB_KEY =
      Functions.compose(INFO_TO_JOB_KEY, ASSIGNED_TO_INFO);

  public static final Function<ScheduledTask, String> SCHEDULED_TO_JOB_KEY =
      Functions.compose(ASSIGNED_TO_JOB_KEY, SCHEDULED_TO_ASSIGNED);

  /**
   * Different states that an active task may be in.
   */
  public static final EnumSet<ScheduleStatus> ACTIVE_STATES = EnumSet.of(
      PENDING, ASSIGNED, STARTING, RUNNING, KILLING, RESTARTING, UPDATING, ROLLBACK, PREEMPTING);

  /**
   * Terminal states, which a task should not move from.
   */
  public static final Set<ScheduleStatus> TERMINAL_STATES = EnumSet.of(
      FAILED, FINISHED, KILLED, LOST
  );

  /**
   * Filter that includes only active tasks.
   */
  public static final Predicate<ScheduledTask> ACTIVE_FILTER = new Predicate<ScheduledTask>() {
      @Override public boolean apply(ScheduledTask task) {
        return isActive(task.getStatus());
      }
    };

  public static final Predicate<TwitterTaskInfo> IS_PRODUCTION =
      new Predicate<TwitterTaskInfo>() {
        @Override public boolean apply(TwitterTaskInfo task) {
          return task.isProduction();
        }
      };

  /**
   * Order by production flag (true, then false), subsorting by task ID.
   */
  public static final Ordering<AssignedTask> SCHEDULING_ORDER =
      Ordering.explicit(true, false)
          .onResultOf(Functions.compose(Functions.forPredicate(IS_PRODUCTION), ASSIGNED_TO_INFO))
          .compound(Ordering.natural().onResultOf(ASSIGNED_TO_ID));

  private Tasks() {
    // Utility class.
  }

  /**
   * Creates a predicate that tests whether other {@link ScheduledTask}s have a status equal to
   * {@code status}.
   *
   * @param status Status to compare against other tasks.
   * @return A new filter that will match other tasks with the same status.
   */
  public static Predicate<ScheduledTask> hasStatus(final ScheduleStatus status) {
    Preconditions.checkNotNull(status);

    return new Predicate<ScheduledTask>() {
      @Override public boolean apply(ScheduledTask task) {
        return task.getStatus() == status;
      }
    };
  }

  public static boolean isActive(ScheduleStatus status) {
    return ACTIVE_STATES.contains(status);
  }

  public static boolean isTerminated(ScheduleStatus status) {
    return TERMINAL_STATES.contains(status);
  }

  public static String jobKey(Identity owner, String jobName) {
    return jobKey(owner.getRole(), jobName);
  }

  public static String jobKey(String role, String jobName) {
    return role + "/" + jobName;
  }

  public static String jobKey(TwitterTaskInfo task) {
    return jobKey(task.getOwner(), task.getJobName());
  }

  public static String jobKey(JobConfiguration job) {
    return jobKey(job.getOwner(), job.getName());
  }

  public static String jobKey(AssignedTask task) {
    return jobKey(task.getTask());
  }

  public static String jobKey(ScheduledTask task) {
    return jobKey(task.getAssignedTask());
  }

  public static String id(ScheduledTask task) {
    return task.getAssignedTask().getTaskId();
  }
}
