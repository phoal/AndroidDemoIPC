package phoal.piko.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import phoal.piko.R;
import phoal.piko.common.GenericActivity;
import phoal.piko.model.ServiceModel;
import phoal.piko.presenter.DataPresenter;
import phoal.piko.utils.loader.ImageLoader;

/**
 * Created by Foal on 15/11/2015.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    public static final String SINGLE_MSG = "Are you sure you want to delete this file?";
    public static final String ALL_MSG = "Are you sure you want to delete ALL FILES?";
    public static final String REFERENCE = "Reference";
    public static final String UNSPECIFIED = "Unspecified";
    public static final Date DATE = new GregorianCalendar(1900,0,1).getTime();

    private SortedList<Image> mPaths;
    private Activity mContext;
    private int mColWidth;
    private ImageLoader mLoader;


    /**
     * @class
     * Provide a reference to the views for each data item
     * Complex data items may need more than one view per item, and you provide
     * EFFICIENT SCROLLING access to all these views in a holder (via one time only lookup)
     */
    public static class ViewHolder  extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public RelativeLayout frame;
        public ImageView thumb;
        public TextView desc;
        public TextView x;
        public ViewHolder(RelativeLayout v) {
            super(v);
            frame = v;
            thumb = (ImageView) v.findViewById(R.id.imageView);
            desc = (TextView) v.findViewById(R.id.desc);
            x = (TextView) v.findViewById(R.id.x);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ImageAdapter(Activity context, ImageLoader loader, int colWidth) {
        mContext = context; //Safe reference - ImageAdapter object destroyed with owning Activity
        mColWidth = colWidth;
        mLoader = loader;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        RelativeLayout v = (RelativeLayout) LayoutInflater.from(parent.getContext())
            .inflate(R.layout.thumbnail, parent, false);


        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        // set up the view's size, margins, paddings and layout parameters
        int adjust = DisplayImagesActivity.ADJUST;

        holder.desc.setText(mPaths.get(position).iDescr);
        // Make sure label and x are set properly.
        holder.frame.setLayoutParams(new RecyclerView.LayoutParams(mColWidth,
                mColWidth + DisplayImagesActivity.ADJUST_PARAM));
        holder.x.setWidth(DisplayImagesActivity.xWIDTH);
        // Set configuration properties of the ImageView
        holder.thumb.setLayoutParams(new RelativeLayout.LayoutParams(mColWidth - adjust,
                mColWidth - adjust));
        holder.thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Implement onClick to start an a SwipeListDisplay at the current image.
        holder.thumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContext.startActivity(new Intent(mContext,
                        ViewPagerActivity.class).setData(ServiceModel.DIR_PATH).putExtra(
                        ViewPagerActivity.CURRENT_IMAGE_POSITION, position));
            }
        });
        /**
         * Create AsyncTasks to load all the images via ImageLoader class
         */
        mLoader.loadAndDisplayImage(holder.thumb, mPaths.get(position).iFile.getAbsolutePath(), mColWidth);

        // Implement Delete listener
        holder.x.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialogFragment.newInstance(position).show(mContext.getFragmentManager(), "Alert");
            }
        });
    }
    public void setImages(Boolean init) {
        File[] files = new File(ServiceModel.DIR_PATH.getPath()).listFiles();
        if (init) {
            /**
             * First call initialises a SortedList - images need to be sorted by date.
             */
            mPaths = new SortedList<Image>(Image.class, new SortedList.Callback<Image>() {
                @Override
                public int compare(Image o1, Image o2) {
                    return o1.iDate.compareTo(o2.iDate);
                }

                @Override
                public void onInserted(int position, int count) {}

                @Override
                public void onRemoved(int position, int count) {}

                @Override
                public void onMoved(int fromPosition, int toPosition) {}

                @Override
                public void onChanged(int position, int count) {}

                @Override
                public boolean areContentsTheSame(Image oldItem, Image newItem) {
                    return oldItem.iFile.getName().equals(newItem.iFile.getName());
                }

                @Override
                public boolean areItemsTheSame(Image item1, Image item2) {
                    return item1.iFile.getName().equals(item2.iFile.getName());
                }
            });
        } else mPaths.clear();
        if (files != null) {
            for (File file : files) {
                mPaths.add(new Image(file));
            }
        }
        notifyDataSetChanged();
    }
    private void setUpDelete(Context context, View delete) {

    }
    public void addImage(Image image) {
        mPaths.add(image);
        notifyDataSetChanged();
    }
    public void deleteFile(int index) {
        if (index == -1) {
            mPaths.clear();
            DataPresenter.deleteFiles(ServiceModel.DIR_PATH, 0);
        }
        else {
            mPaths.get(index).iFile.delete();
            mPaths.removeItemAt(index);
        }
        notifyDataSetChanged();
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mPaths.size();
    }
    public void showAlert(int index) {
        AlertDialogFragment.newInstance(index).show(mContext.getFragmentManager(), "Alert");
    }

    // Class that creates the AlertDialog to check that user wants to delete a file;

    /**
     * Create the dialog to confirm delete
     * Valid only for DisplayImagesActivity - *** NOTE CAST in setPositiveButton() ***
     */
    public static class AlertDialogFragment extends DialogFragment {
        /**
         * @param index The path of file to be deleted
         * @return The "Confirm delete?" dialog
         */
        public static AlertDialogFragment newInstance(int index) {
            Bundle b = new Bundle();
            b.putInt(DisplayImagesActivity.PATH, index);
            AlertDialogFragment alert = new AlertDialogFragment();
            alert.setArguments(b);
            return alert;
        }

        // Build AlertDialog using AlertDialog.Builder
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Retrieve file path
            final int idx = getArguments().getInt(DisplayImagesActivity.PATH);
            final String msg = idx == -1 ? ALL_MSG : SINGLE_MSG;
            return new AlertDialog.Builder(getActivity())
                    .setMessage(msg)
                            // User cannot dismiss dialog by hitting back button.
                    .setCancelable(false)

                            // Set up No Button
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    AlertDialogFragment.this.dismiss();
                                }
                            })
                            // Set up Yes Button
                    .setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, int id) {
                                    // *** Call the fragments getActivity() - this MUST be attached
                                    // to a DisplayImagesActivity ***
                                    DisplayImagesActivity display = (DisplayImagesActivity)getActivity();
                                    display.getImageAdapter().deleteFile(idx);
                                }
                            }).create();
        }
    }
    public static class Image {
        public final Date iDate;
        public final File iFile;
        public final String iDescr;

        public Image(File file) {
            Date date;
            String desc;
            if (file.getName().startsWith("00")) {
                date = DATE;
                desc = REFERENCE;
            }
            else try {
                date = new SimpleDateFormat("yyyyMMddHHmmss").parse(file.getName().substring(0, 14));
                desc = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss").format(date);
            } catch(Exception e) {
                date = DATE;
                desc = UNSPECIFIED;
            }
            this.iDate = date;
            this.iFile = file;
            this.iDescr = desc;
        }
    }
}
