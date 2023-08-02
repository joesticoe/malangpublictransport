package ap.mobile.malangpublictransport;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import ap.mobile.malangpublictransport.base.RouteTransport;
import ap.mobile.malangpublictransport.utilities.Helper;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.ViewHolder> {

    private RouteAdapterItemClickListener listener;
    private ArrayList<RouteTransport> routes;

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvRouteName;
        TextView tvPrice;
        TextView tvDistance;
        ImageView ivLine;
        View itemView;

        ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            this.tvRouteName = itemView.findViewById(R.id.route_name);
            this.tvPrice = itemView.findViewById(R.id.route_price);
            this.tvDistance = itemView.findViewById(R.id.route_distance);
            this.ivLine = itemView.findViewById(R.id.route_icon);
        }

        void bind(final RouteTransport routeTransport, final RouteAdapterItemClickListener listener) {
            this.tvRouteName.setText(routeTransport.getNames());
            this.tvDistance.setText(Helper.humanReadableDistane(routeTransport.getDistanceReadable()));
            String priceLabel = "Rp " + String.format(Locale.getDefault(), "%,.0f", routeTransport.getTotalPrice()).replace(",", ".");
            this.tvPrice.setText(priceLabel);
            this.ivLine.setColorFilter(Color.parseColor("#2196f3"));
            this.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null)
                        listener.onItemClick(routeTransport);
                }
            });
        }
    }

    public interface RouteAdapterItemClickListener {
        void onItemClick(RouteTransport routeTransport);
    }

    RouteAdapter(ArrayList<RouteTransport> routes, RouteAdapterItemClickListener listener) {
        this.routes = routes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route_selected, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RouteTransport routeTransport = this.routes.get(position);
        holder.bind(routeTransport, this.listener);
    }

    @Override
    public int getItemCount() {
        return this.routes.size();
    }

}
