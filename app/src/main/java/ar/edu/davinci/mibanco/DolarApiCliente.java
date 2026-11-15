package ar.edu.davinci.mibanco;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Crea una única instancia de Retrofit para toda la app.
 * Crear un cliente por request desperdiciaría conexiones y memoria.
 */
public final class DolarApiCliente {

    private static final String BASE_URL = "https://dolarapi.com/";

    private static DolarApi instancia;

    private DolarApiCliente() { }

    public static synchronized DolarApi get() {
        if (instancia == null) {
            instancia = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(DolarApi.class);
        }
        return instancia;
    }
}