package com.jsoniter.extra;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.Until;
import com.jsoniter.JsonIterator;
import com.jsoniter.ValueType;
import com.jsoniter.annotation.JsonIgnore;
import com.jsoniter.annotation.JsonProperty;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class GsonCompatibilityMode extends Config {


	private static final int SURR1_FIRST = 0xD800;
	private static final int SURR1_LAST = 0xDBFF;
	private static final int SURR2_FIRST = 0xDC00;
	private static final int SURR2_LAST = 0xDFFF;
	private static final String[] REPLACEMENT_CHARS;
	private static final String[] HTML_SAFE_REPLACEMENT_CHARS;


	static {
		REPLACEMENT_CHARS = new String[128];
		for (int i = 0; i <= 0x1f; i++) {
			REPLACEMENT_CHARS[i] = String.format("\\u%04x", (int) i);
		}
		REPLACEMENT_CHARS['"'] = "\\\"";
		REPLACEMENT_CHARS['\\'] = "\\\\";
		REPLACEMENT_CHARS['\t'] = "\\t";
		REPLACEMENT_CHARS['\b'] = "\\b";
		REPLACEMENT_CHARS['\n'] = "\\n";
		REPLACEMENT_CHARS['\r'] = "\\r";
		REPLACEMENT_CHARS['\f'] = "\\f";
		HTML_SAFE_REPLACEMENT_CHARS = REPLACEMENT_CHARS.clone();
		HTML_SAFE_REPLACEMENT_CHARS['<'] = "\\u003c";
		HTML_SAFE_REPLACEMENT_CHARS['>'] = "\\u003e";
		HTML_SAFE_REPLACEMENT_CHARS['&'] = "\\u0026";
		HTML_SAFE_REPLACEMENT_CHARS['='] = "\\u003d";
		HTML_SAFE_REPLACEMENT_CHARS['\''] = "\\u0027";
	}

	private GsonCompatibilityMode(String configName, ReBuilder builder) {
		super(configName, builder);
	}

	@Override
	protected RiBuilder builder() {
		return (RiBuilder) super.builder();
	}

	public static class RiBuilder extends Config.ReBuilder {
		private boolean excludeFieldsWithoutExposeAnnotation = false;
		private boolean disableHtmlEscaping = false;
		private ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
			@Override
			protected DateFormat initialValue() {
				dateFormat.remove();
				return DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.US);
			}
		};
		private FieldNamingStrategy fieldNamingStrategy;
		private Double version;
		private Set<ExclusionStrategy> serializationExclusionStrategies = new HashSet<ExclusionStrategy>();
		private Set<ExclusionStrategy> deserializationExclusionStrategies = new HashSet<ExclusionStrategy>();

		public void reBuilder() {
			omitDefaultValue(true);
		}

		@Override
		public ReBuilder excludeFieldsWithoutExposeAnnotation() {
			excludeFieldsWithoutExposeAnnotation = true;
			return this;
		}

		@Override
		public ReBuilder serializeNulls() {
			omitDefaultValue(false);
			return this;
		}

		@Override
		public ReBuilder setDateFormat() {
			// no op, same as gson
			return this;
		}

		
		public ReBuilder setDateFormat(final int dateStyle, final int timeStyle) {
			dateFormat = new ThreadLocal<DateFormat>() {
				@Override
				protected DateFormat initialValue() {
					return DateFormat.getDateTimeInstance(dateStyle, timeStyle, Locale.US);
				}
			};
			return this;
		}

		
		public ReBuilder setDateFormat(final String pattern) {
			dateFormat = new ThreadLocal<DateFormat>() {
				@Override
				protected DateFormat initialValue() {
					return new SimpleDateFormat(pattern, Locale.US);
				}
			};
			return this;
		}

		public ReBuilder setFieldNamingStrategy(FieldNamingStrategy fieldNamingStrategy) {
			this.fieldNamingStrategy = fieldNamingStrategy;
			return this;
		}

		public ReBuilder setFieldNamingPolicy(FieldNamingPolicy namingConvention) {
			this.fieldNamingStrategy = namingConvention;
			return this;
		}

		@Override
		public ReBuilder setPrettyPrinting() {
			indentionStep(2);
			return this;
		}

		@Override
		public ReBuilder disableHtmlEscaping() {
			disableHtmlEscaping = true;
			return this;
		}

		
		public ReBuilder setVersion(double version) {
			this.version = version;
			return this;
		}

		public ReBuilder setExclusionStrategies(ExclusionStrategy... strategies) {
			for (ExclusionStrategy strategy : strategies) {
				addSerializationExclusionStrategy(strategy);
			}
			return this;
		}

		public ReBuilder addSerializationExclusionStrategy(ExclusionStrategy exclusionStrategy) {
			serializationExclusionStrategies.add(exclusionStrategy);
			return this;
		}

		public ReBuilder addDeserializationExclusionStrategy(ExclusionStrategy exclusionStrategy) {
			deserializationExclusionStrategies.add(exclusionStrategy);
			return this;
		}

		@Override
		public GsonCompatibilityMode build() {
			escapeUnicode(false);
			return (GsonCompatibilityMode) super.build();
		}

		@Override
		protected Config doBuild(String configName) {
			return new GsonCompatibilityMode(configName, this);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			if (!super.equals(o))
				return false;

			RiBuilder builder = (RiBuilder) o;

			return excludeFieldsWithoutExposeAnnotation == builder.excludeFieldsWithoutExposeAnnotation
					&& disableHtmlEscaping == builder.disableHtmlEscaping
					&& dateFormat.get().equals(builder.dateFormat.get())
					&& (fieldNamingStrategy != null ? fieldNamingStrategy.equals(builder.fieldNamingStrategy)
							: builder.fieldNamingStrategy == null)
					&& (version != null ? version.equals(builder.version) : builder.version == null)
					&& (serializationExclusionStrategies != null
							? serializationExclusionStrategies.equals(builder.serializationExclusionStrategies)
							: builder.serializationExclusionStrategies == null)
					&& (deserializationExclusionStrategies != null
							? deserializationExclusionStrategies.equals(builder.deserializationExclusionStrategies)
							: builder.deserializationExclusionStrategies == null);
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (excludeFieldsWithoutExposeAnnotation ? 1 : 0);
			result = 31 * result + (disableHtmlEscaping ? 1 : 0);
			result = 31 * result + dateFormat.get().hashCode();
			result = 31 * result + (fieldNamingStrategy != null ? fieldNamingStrategy.hashCode() : 0);
			result = 31 * result + (version != null ? version.hashCode() : 0);
			result = 31 * result
					+ (serializationExclusionStrategies != null ? serializationExclusionStrategies.hashCode() : 0);
			result = 31 * result
					+ (deserializationExclusionStrategies != null ? deserializationExclusionStrategies.hashCode() : 0);
			return result;
		}

		@Override
		public Config.ReBuilder copy() {
			RiBuilder copied = (RiBuilder) super.copy();
			copied.excludeFieldsWithoutExposeAnnotation = excludeFieldsWithoutExposeAnnotation;
			copied.disableHtmlEscaping = disableHtmlEscaping;
			copied.dateFormat = dateFormat;
			copied.fieldNamingStrategy = fieldNamingStrategy;
			copied.version = version;
			copied.serializationExclusionStrategies = new HashSet<ExclusionStrategy>(serializationExclusionStrategies);
			copied.deserializationExclusionStrategies = new HashSet<ExclusionStrategy>(
					deserializationExclusionStrategies);
			return copied;
		}

		@Override
		public String toString() {
			return super.toString() + " => GsonCompatibilityMode{" + "excludeFieldsWithoutExposeAnnotation="
					+ excludeFieldsWithoutExposeAnnotation + ", disableHtmlEscaping=" + disableHtmlEscaping
					+ ", dateFormat=" + dateFormat + ", fieldNamingStrategy=" + fieldNamingStrategy + ", version="
					+ version + ", serializationExclusionStrategies=" + serializationExclusionStrategies
					+ ", deserializationExclusionStrategies=" + deserializationExclusionStrategies + '}';
		}
	}

	@Override
	protected OmitValue createOmitValue(Type valueType) {
		if (valueType instanceof Class) {
			Class clazz = (Class) valueType;
			if (clazz.isPrimitive()) {
				return null; // gson do not omit primitive zero
			}
		}
		return super.createOmitValue(valueType);
	}

	@Override
	public Encoder createEncoder(String cacheKey, Type type) {
				
		if (Date.class == type) {
			return new Encoder() {
				@Override
				public void encode(Object obj, JsonStream stream) throws IOException {
					DateFormat dateFormat = builder().dateFormat.get();
					stream.writeVal(dateFormat.format(obj));
				}
			};
		} else if (String.class == type) {
			final String[] replacements;
			if (builder().disableHtmlEscaping) {
				replacements = REPLACEMENT_CHARS;
			} else {
				replacements = HTML_SAFE_REPLACEMENT_CHARS;
			}
			return new Encoder() {
				@Override
				public void encode(Object obj, JsonStream stream) throws IOException {
					String value = (String) obj;
					stream.write('"');
					int surrogat = 0;
					
					encod(replacements, value, stream, surrogat);
					
					stream.write('"');
				}
			};
		}
		return super.createEncoder(cacheKey, type);
	}
      
	private void encod(String[] replacements, String value, JsonStream stream, int surrogat) throws IOException {
		
		for (int i = 0; i < value.length(); i++) {
			int c = value.charAt(i);
			String replacement;

			if (c < 128) {
				replacement = replacements[c];
				if (replacement == null) {
					stream.write(c);
				} else {
					stream.writeRaw(replacement);
				}
			} else if (c == '\u2028') {
				stream.writeRaw("\\u2028");
			} else if (c == '\u2029') {
				stream.writeRaw("\\u2029");
			} else {
				if (c < 0x800) { // 2-byte
					stream.write((byte) (0xc0 | (c >> 6)), (byte) (0x80 | (c & 0x3f)));
				} else { // 3 or 4 bytes
					// Surrogates?

					enc2(c, stream, surrogat, value, i);

				}
			}
		}
	}
	private void enc2(int c,  JsonStream stream, int surrogat, String value, int i) throws IOException {
    	
    	 if (c < SURR1_FIRST || c > SURR2_LAST) {
             stream.write(
                     (byte) (0xe0 | (c >> 12)),
                     (byte) (0x80 | ((c >> 6) & 0x3f)),
                     (byte) (0x80 | (c & 0x3f))
             );
             
         }
         // Yup, a surrogate:
         if (c > SURR1_LAST) { // must be from first range
             throw new JsonException("illegalSurrogate");
         }
         
         // and if so, followed by another from next range
         if (i >= value.length()) { // unless we hit the end?
             
         }
         i++;
         c = value.charAt(i);
         int firstPart = surrogat;
         
         // Ok, then, is the second part valid?
         if (c < SURR2_FIRST || c > SURR2_LAST) {
             throw new JsonException("Broken surrogate pair: first char 0x" + Integer.toHexString(firstPart) + ", second 0x" + Integer.toHexString(c) + "; illegal combination");
         }
         c = 0x10000 + ((firstPart - SURR1_FIRST) << 10) + (c - SURR2_FIRST);
         if (c > 0x10FFFF) { // illegal in JSON as well as in XML
             throw new JsonException("illegalSurrogate");
         }
         stream.write(
                 (byte) (0xf0 | (c >> 18)),
                 (byte) (0x80 | ((c >> 12) & 0x3f)),
                 (byte) (0x80 | ((c >> 6) & 0x3f)),
                 (byte) (0x80 | (c & 0x3f))
         );
    }

	@Override
	public Decoder createDecoder(String cacheKey, Type type) {
		if (Date.class == type) {
			return new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					DateFormat dateFormat = builder().dateFormat.get();
					try {
						String input = iter.readString();
						return dateFormat.parse(input);
					} catch (ParseException e) {
						throw new JsonException(e);
					}
				}
			};
		} else if (String.class == type) {			
			decoder1();
			
		} else if (boolean.class == type) {			
			decoder2();
			
		} else if (long.class == type) {
			decoder3();
			
		} else if (int.class == type) {
			decoder4();
			
		} else if (float.class == type) {
			decoder5();
			
		} else if (double.class == type) {
			decoder6();
			
		}
		return super.createDecoder(cacheKey, type);
	}  
	
	private static Decoder decoder1() {
		
		return new Decoder() {
			@Override
			public Object decode(JsonIterator iter) throws IOException {
				ValueType valueType = iter.whatIsNext();
				if (valueType == ValueType.STRING) {
					return iter.readString();
				} else if (valueType == ValueType.NUMBER) {
					return iter.readNumberAsString();
				} else if (valueType == ValueType.BOOLEAN) {
					return iter.readBoolean() ? "true" : "false";
				} else if (valueType == ValueType.NULL) {
					iter.skip();
					return null;
				} else {
					throw new JsonException("expect string, but found " + valueType);
				}
			}
		};
	}
	

	private static Decoder decoder2() {
		
		return new Decoder.BooleanDecoder() {
			@Override
			public boolean decodeBoolean(JsonIterator iter) throws IOException {
				ValueType valueType = iter.whatIsNext();
				if (valueType == ValueType.BOOLEAN) {
					return iter.readBoolean();
				} else if (valueType == ValueType.NULL) {
					iter.skip();
					return false;
				} else {
					throw new JsonException("expect boolean, but found " + valueType);
				}
			}
		};
	}
	
	private static Decoder decoder3() {
		
		return new Decoder.LongDecoder() {
			@Override
			public long decodeLong(JsonIterator iter) throws IOException {
				ValueType valueType = iter.whatIsNext();
				if (valueType == ValueType.NUMBER) {
					return iter.readLong();
				} else if (valueType == ValueType.NULL) {
					iter.skip();
					return 0;
				} else {
					throw new JsonException("expect long, but found " + valueType);
				}
			}  
		};
	}

	private static Decoder decoder4() {
		
		return new Decoder.IntDecoder() {
			@Override
			public int decodeInt(JsonIterator iter) throws IOException {
				ValueType valueType = iter.whatIsNext();
				if (valueType == ValueType.NUMBER) {
					return iter.readInt();
				} else if (valueType == ValueType.NULL) {
					iter.skip();
					return 0;
				} else {
					throw new JsonException("expect int, but found " + valueType);
				}
			}
		};
	}
	
	private static Decoder decoder5() {
		
		return new Decoder.FloatDecoder() {
			@Override
			public float decodeFloat(JsonIterator iter) throws IOException {
				ValueType valueType = iter.whatIsNext();
				if (valueType == ValueType.NUMBER) {
					return iter.readFloat();
				} else if (valueType == ValueType.NULL) {
					iter.skip();
					return 0.0f;
				} else {
					throw new JsonException("expect float, but found " + valueType);
				}
			}
		};
	}
	
	private static Decoder decoder6() {
		
		return new Decoder.DoubleDecoder() {
			@Override
			public double decodeDouble(JsonIterator iter) throws IOException {
				ValueType valueType = iter.whatIsNext();
				if (valueType == ValueType.NUMBER) {
					return iter.readDouble();
				} else if (valueType == ValueType.NULL) {
					iter.skip();
					return 0.0d;
				} else {
					throw new JsonException("expect float, but found " + valueType);
				}
			}
		};
	}
	

	@Override
	public void updateClassDescriptor(ClassDescriptor desc) {
		FieldNamingStrategy fieldNamingStrategy = builder().fieldNamingStrategy;
		for (Binding binding : desc.allBindings()) {
			update(binding, fieldNamingStrategy);
			
			update2(binding);
			for (ExclusionStrategy strategy : builder().deserializationExclusionStrategies) {

				if (strategy.shouldSkipClass(binding.clazz)) {
					binding.setFromNames(new String[0]);

					continue;   
				}
				if (strategy.shouldSkipField(new FieldAttributes(binding.getField()))) {
					binding.setFromNames(new String[0]);
				}
			}
		}
		super.updateClassDescriptor(desc);
	}
	
	private void update2(Binding binding) {
		
		for (ExclusionStrategy strategy : builder().serializationExclusionStrategies) {

			if (strategy.shouldSkipClass(binding.clazz)) {
				binding.setToNames(new String[0]);

				continue;
			}
			if (strategy.shouldSkipField(new FieldAttributes(binding.getField()))) {
				binding.setToNames(new String[0]);
			}
		}
	}
	   
	private void update(Binding binding, FieldNamingStrategy fieldNamingStrategy) {
		
		if (binding.getMethod() != null) {
			binding.setToNames(new String[0]);
			binding.setFromNames(new String[0]);
		}
		if (fieldNamingStrategy != null && binding.getField() != null) {
			String translated = fieldNamingStrategy.translateName(binding.getField());
			binding.setToNames(new String[] { translated });
			binding.setFromNames(new String[] { translated });
		}
		if (builder().version != null) {
			Since since = binding.getAnnotation(Since.class);
			if (since != null && builder().version < since.value()) {
				binding.setToNames(new String[0]);
				binding.setFromNames(new String[0]);
			}
			Until until = binding.getAnnotation(Until.class);
			if (until != null && builder().version >= until.value()) {
				binding.setToNames(new String[0]);
				binding.setFromNames(new String[0]);
			}
		}
	}

	@Override
	protected JsonProperty getJsonProperty(Annotation[] annotations) {
		JsonProperty jsoniterObj = super.getJsonProperty(annotations);
		if (jsoniterObj != null) {
			return jsoniterObj;
		}
		final SerializedName gsonObj = getAnnotation(annotations, SerializedName.class);
		if (gsonObj == null) {
			return null;
		}
		return new JsonProperty() {



			@Override
			public String[] to() {
				return new String[] { gsonObj.value() };
			}
			@Override
			public String value() {
				return "";
			}


			@Override
			public String defaultValueToOmit() {
				return "";
			}
			@Override
			public Class<? extends Encoder> encoder() {
				return Encoder.class;
			}	
			@Override
			public String[] from() {
				return new String[] { gsonObj.value() };
			}
			@Override
			public boolean required() {
				return false;
			}
			@Override
			public boolean collectionValueNullable() {
				return true;
			}

			@Override
			public Class<? extends Decoder> decoder() {
				return Decoder.class;
			}

			@Override
			public boolean nullable() {
				return true;
			}
			@Override
			public Class<?> implementation() {
				return Object.class;
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return JsonProperty.class;
			}
		};
	}

	@Override
	protected JsonIgnore getJsonIgnore(Annotation[] annotations) {

		JsonIgnore jsoniterObj = super.getJsonIgnore(annotations);
		if (jsoniterObj != null) {
			return jsoniterObj;
		}
		if (builder().excludeFieldsWithoutExposeAnnotation) {
			final Expose gsonObj = getAnnotation(annotations, Expose.class);
			if (gsonObj != null) {
				return new JsonIgnore() {
					@Override
					public boolean ignoreDecoding() {
						return !gsonObj.deserialize();
					}

					@Override
					public boolean ignoreEncoding() {
						return !gsonObj.serialize();
					}

					@Override
					public Class<? extends Annotation> annotationType() {
						return JsonIgnore.class;
					}
				};
			}
			return new JsonIgnore() {
				@Override
				public boolean ignoreDecoding() {
					return true;
				}

				@Override
				public boolean ignoreEncoding() {
					return true;
				}

				@Override
				public Class<? extends Annotation> annotationType() {
					return JsonIgnore.class;
				}
			};
		}
		return null;
	}
}
