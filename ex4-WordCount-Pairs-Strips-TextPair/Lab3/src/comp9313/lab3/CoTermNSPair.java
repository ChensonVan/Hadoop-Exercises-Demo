package comp9313.lab3;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


public class CoTermNSPair {

	public static class TextPair implements WritableComparable<TextPair> {
		private Text first;
		private Text second;
		
		public TextPair() {
			set(new Text(), new Text());
		}
		
		public TextPair(String first, String second) {
			set(new Text(first), new Text(second));
		}
		
		public TextPair(Text first, Text second) {
			set(first, second);
		}
		
		public void set(Text first, Text second) {
			this.first = first;
			this.second = second;
		}

		public Text getFirst() {
			return first;
		}
	  
		public Text getSecond() {
			return second;
		}
	  
		@Override
		public void write(DataOutput out) throws IOException {
			first.write(out);
			second.write(out);
		}
	  
		@Override
		public void readFields(DataInput in) throws IOException {
			first.readFields(in);
			second.readFields(in);
		}
	  
		@Override
		public int hashCode() {
			return first.hashCode() * 163  + second.hashCode();
		}
	  
		@Override
		public boolean equals(Object o) {
			if (o instanceof TextPair) {
				TextPair tp = (TextPair) o;
				return  (first == tp.first) && (second == tp.second);
			}
			return false;
		}
		
		@Override
		public String toString() {
			return first + "\t" + second;
		}
	  
		@Override
		public int compareTo(TextPair tp) {
			int cmp = first.compareTo(tp.first);
			if (cmp != 0) {
				return cmp;
			}
			return second.compareTo(tp.second);
		}

		public static class Comparator extends WritableComparator {
			public static final Text.Comparator TEXT_COMPARATOR = new Text.Comparator();
			
			public Comparator() {
				super(TextPair.class);
			}
		  
			@Override
			public int compare(byte[] b1, int s1, int l1,
							   byte[] b2, int s2, int l2) {
				try{
					int firstL1 = WritableUtils.decodeVIntSize(b1[s1]) + readVInt(b1, s1);
					int firstL2 = WritableUtils.decodeVIntSize(b2[s2]) + readVInt(b2, s2);
					
					int cmp = TEXT_COMPARATOR.compare(b1, s1, firstL1, b2, s2, firstL2);
					if (cmp != 0) {
						return cmp;
					}
					return TEXT_COMPARATOR.compare(b1, s1 + firstL1, l1 - firstL1, b2, s2 + firstL2, l2 - firstL2);
				} catch (IOException e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
		
		static {
			WritableComparator.define(TextPair.class, new Comparator());
		}
		
		public static class FirstComparator extends WritableComparator {
			private static final Text.Comparator TEXT_COMPARATOR = new Text.Comparator();
			
			public FirstComparator() {
				super(TextPair.class);
			}
			
			@Override
			public int compare(byte[] b1, int s1, int l1,
							   byte[] b2, int s2, int l2) {
				try{
					int firstL1 = WritableUtils.decodeVIntSize(b1[s1]) + readVInt(b1, s1);
					int firstL2 = WritableUtils.decodeVIntSize(b2[s2]) + readVInt(b2, s2);
					
//					int cmp = TEXT_COMPARATOR.compare(b1, s1, firstL1, b2, s2, firstL2);
//					if (cmp != 0) {
//						return cmp;
//					}
//					return TEXT_COMPARATOR.compare(b1, s1 + firstL1, l1 - firstL1, b2, s2 + firstL2, l2 - firstL2);
					return TEXT_COMPARATOR.compare(b1, s1, firstL1, b2, s2, firstL2);
				} catch (IOException e) {
					throw new IllegalArgumentException(e);
				}
			}
			
			@Override
			public int compare(WritableComparable a, WritableComparable b) {
				if (a instanceof TextPair && b instanceof TextPair ) {
					return ((TextPair) a).first.compareTo(((TextPair) b).first);
				}
				return super.compare(a, b);
			}
		}
	} 	// end of TextPair	
	
	public static class TokenizerMapper extends Mapper<Object, Text, TextPair, IntWritable> {

		private final static IntWritable counter = new IntWritable(1);
		private Text word = new Text();		
		private TextPair textPair = new TextPair();
		
		// private List<Text> preWord = new ArrayList<Text>();

	    public void map(Object key, Text value, Context context
	                    ) throws IOException, InterruptedException {
	      StringTokenizer itr = new StringTokenizer(value.toString());
          private List<Text> preWord = new ArrayList<Text>();
	      while (itr.hasMoreTokens()) {
	        word.set(itr.nextToken().toLowerCase());
	        for(Text pWord : preWord) {
	        	textPair.set(pWord, word);
		        context.write(textPair, counter);
	        }
	        preWord.add(new Text(word.toString()));
	      }
	    }
	}

	public static class IntSumReducer extends Reducer<TextPair, IntWritable, TextPair, IntWritable> {
		private IntWritable result = new IntWritable();

		public void reduce(TextPair key, Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable val : values) {
				sum += val.get();
			}
			result.set(sum);
			context.write(key, result);
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "word count");
		job.setJarByClass(WordCount3.class);
		job.setMapperClass(TokenizerMapper.class);
		job.setReducerClass(IntSumReducer.class);
		job.setMapOutputKeyClass(TextPair.class);
		job.setMapOutputValueClass(IntWritable.class);
		job.setOutputKeyClass(TextPair.class);
		job.setOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
