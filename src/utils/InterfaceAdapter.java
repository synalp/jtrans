package utils;

import java.lang.reflect.Type;

import com.google.gson.*;

/**
 * For JSON serialization.
 *
 * Adds an extra property for class names. Strips package names to save
 * space - therefore all subclasses of T must be in the same package as T!
 */
public class InterfaceAdapter<T>
		implements JsonSerializer<T>, JsonDeserializer<T>
{
	private String typePropertyName;

	public InterfaceAdapter(String typePropertyName) {
		this.typePropertyName = typePropertyName;
	}

	@Override
	public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject jo = context.serialize(src).getAsJsonObject();
		jo.addProperty(typePropertyName, src.getClass().getSimpleName());
		return jo;
	}

	@Override
	public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException
	{
		JsonObject jo = json.getAsJsonObject();
		String packageName = ((Class)typeOfT).getPackage().getName();
		Type type;

		JsonElement typeJson = jo.get(typePropertyName);
		if (typeJson == null)
			throw new JsonParseException("missing " + typePropertyName + " property");

		try {
			type = Class.forName(packageName + "." + typeJson.getAsString());
		} catch (ClassNotFoundException e) {
			throw new JsonParseException(e);
		}

		// don't let the deserializer touch the type property
		jo.remove(typePropertyName);

		return context.deserialize(jo, type);
	}
}