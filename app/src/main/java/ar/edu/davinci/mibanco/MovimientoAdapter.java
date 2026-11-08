package ar.edu.davinci.mibanco;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MovimientoAdapter
        extends RecyclerView.Adapter<MovimientoAdapter.MovimientoViewHolder> {

    private static final Locale LOCALE_AR = Locale.forLanguageTag("es-AR");

    private final List<Movimiento> movimientos;

    public MovimientoAdapter(List<Movimiento> movimientos) {
        this.movimientos = movimientos;
    }

    @NonNull
    @Override
    public MovimientoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movimiento, parent, false);
        return new MovimientoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull MovimientoViewHolder holder, int position) {
        holder.bind(movimientos.get(position));
    }

    @Override
    public int getItemCount() {
        return movimientos.size();
    }

    static class MovimientoViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivAvatar;
        private final ImageView ivBadge;
        private final TextView tvTitulo;
        private final TextView tvFecha;
        private final TextView tvMonto;

        MovimientoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivBadge = itemView.findViewById(R.id.ivBadge);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvMonto = itemView.findViewById(R.id.tvMonto);
        }

        void bind(Movimiento movimiento) {
            Context ctx = itemView.getContext();
            boolean ingreso = movimiento.esIngreso();

            // Descarga la imagen desde la URL; mientras carga o si falla,
            // muestra el avatar por defecto. Glide la cachea en memoria y disco.
            Glide.with(ivAvatar)
                    .load(movimiento.getAvatarUrl())
                    .placeholder(R.drawable.avatar_default)
                    .error(R.drawable.avatar_default)
                    .centerCrop()
                    .into(ivAvatar);

            ivBadge.setImageResource(
                    ingreso ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up);
            ivBadge.setBackgroundResource(
                    ingreso ? R.drawable.bg_circle_teal : R.drawable.bg_circle_navy);

            tvTitulo.setText(movimiento.getTitulo());
            tvFecha.setText(formatearFecha(ctx, movimiento.getFecha()));
            tvMonto.setText(formatearMonto(movimiento));
            tvMonto.setTextColor(ContextCompat.getColor(ctx,
                    ingreso ? R.color.teal : R.color.navy));
        }
    }

    private static String formatearMonto(Movimiento movimiento) {
        NumberFormat formato = NumberFormat.getNumberInstance(LOCALE_AR);
        formato.setMaximumFractionDigits(0);
        String signo = movimiento.esIngreso() ? "+" : "-";
        return signo + "$" + formato.format(Math.abs(movimiento.getMonto()));
    }

    /** "Hoy, 10:32", "Ayer, 18:45" o "6 sep, 11:05". */
    static String formatearFecha(Context ctx, Date fecha) {
        String hora = new SimpleDateFormat("HH:mm", LOCALE_AR).format(fecha);

        Calendar dia = Calendar.getInstance();
        dia.setTime(fecha);
        Calendar referencia = Calendar.getInstance();

        if (mismoDia(dia, referencia)) {
            return ctx.getString(R.string.fecha_hoy, hora);
        }
        referencia.add(Calendar.DAY_OF_YEAR, -1);
        if (mismoDia(dia, referencia)) {
            return ctx.getString(R.string.fecha_ayer, hora);
        }
        return new SimpleDateFormat("d MMM, HH:mm", LOCALE_AR).format(fecha);
    }

    private static boolean mismoDia(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}