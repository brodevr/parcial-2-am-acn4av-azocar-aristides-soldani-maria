package ar.edu.davinci.mibanco;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Endpoints de DolarApi (https://dolarapi.com). Retrofit genera la implementación.
 */
public interface DolarApi {

    /** Todas las cotizaciones del dólar en Argentina: oficial, blue, bolsa (MEP), etc. */
    @GET("v1/dolares")
    Call<List<Cotizacion>> getDolares();
}
