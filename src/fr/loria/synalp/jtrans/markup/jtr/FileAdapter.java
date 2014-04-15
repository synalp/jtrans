package fr.loria.synalp.jtrans.markup.jtr;

import com.google.gson.*;

import java.io.File;
import java.lang.reflect.Type;


/**
 * JSON serializer for files. Workaround for buggy deserialization of vanilla
 * File objects from absolute paths
 */
class FileAdapter
		implements JsonSerializer<File>, JsonDeserializer<File>
{

	@Override
	public JsonElement serialize(
			File src,
			Type typeOfSrc,
			JsonSerializationContext context)
	{
		return new JsonPrimitive(src.getAbsolutePath());
	}


	@Override
	public File deserialize(
			JsonElement json,
			Type typeOfT,
			JsonDeserializationContext context)
			throws JsonParseException
	{
		return new File(json.getAsJsonPrimitive().getAsString());
	}

}
