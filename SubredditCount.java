import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;
import org.apache.hadoop.mapreduce.lib.jobcontrol.JobControl;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SubredditCount {

	public static class Map extends Mapper<Object, Text, Text, IntWritable> {
		private final static IntWritable one = new IntWritable(1);
		private Text subreddit = new Text();

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			JSONParser jsonParser = new JSONParser();
			try {
				JSONObject jsonObject = (JSONObject) jsonParser.parse(value.toString());
				String content = (String) jsonObject.get("content");
				String subredditName = (String) jsonObject.get("subreddit");

				if (content != null && subredditName != null && content.toLowerCase().contains("sweden")) {
					subreddit.set(subredditName);
					context.write(subreddit, one);
				}
			} catch (ParseException e) {
				// JSON parsing error, ignore this record
			}
		}
	}

	public static class Reduce extends Reducer<Text, IntWritable, Text, IntWritable> {
		public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable val : values) {
				sum += val.get();
			}
			context.write(key, new IntWritable(sum));
		}
	}

	public static class SortMap extends Mapper<Object, Text, IntWritable, Text> {
		private Text subreddit = new Text();

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String[] tokens = value.toString().split("\\s+");
			String subredditName = tokens[0];
			int count = Integer.parseInt(tokens[1]);
			subreddit.set(subredditName);
			context.write(new IntWritable(count), subreddit);
		}
	}

	public static class SortReduce extends Reducer<IntWritable, Text, Text, IntWritable> {
		public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			for (Text val : values) {
				context.write(val, key);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();

		// First MapReduce job
		Job job1 = Job.getInstance(conf, "subreddit count");
		job1.setJarByClass(SubredditCount.class);
		job1.setMapperClass(Map.class);
		job1.setCombinerClass(Reduce.class);
		job1.setReducerClass(Reduce.class);
		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(IntWritable.class);
		job1.setInputFormatClass(TextInputFormat.class);
		job1.setOutputFormatClass(TextOutputFormat.class);
		FileInputFormat.addInputPath(job1, new Path(args[0]));
		FileOutputFormat.setOutputPath(job1, new Path(args[1]));

		// Second MapReduce job for sorting
		Job job2 = Job.getInstance(conf, "sort by value");
		job2.setJarByClass(SubredditCount.class);
		job2.setMapperClass(SortMap.class);
		job2.setReducerClass(SortReduce.class);
		job2.setOutputKeyClass(IntWritable.class);
		job2.setOutputValueClass(Text.class);
		job2.setSortComparatorClass(IntWritable.Comparator.class);
		job2.setInputFormatClass(TextInputFormat.class);
		job2.setOutputFormatClass(TextOutputFormat.class);
		FileInputFormat.addInputPath(job2, new Path(args[1])); // Use the output of job1 as input for job2
		FileOutputFormat.setOutputPath(job2, new Path(args[2]));

		// Run jobs
		JobControl jobControl = new JobControl("SubredditCount");
		ControlledJob controlledJob1 = new ControlledJob(job1.getConfiguration());
		controlledJob1.setJob(job1);
		ControlledJob controlledJob2 = new ControlledJob(job2.getConfiguration());
		controlledJob2.setJob(job2);
		controlledJob2.addDependingJob(controlledJob1);

		jobControl.addJob(controlledJob1);
		jobControl.addJob(controlledJob2);

		Thread jobControlThread = new Thread(jobControl);
		jobControlThread.start();

		while (!jobControl.allFinished()) {
			Thread.sleep(5000); // Check every 5 seconds
		}

		jobControl.stop();
	}
}
