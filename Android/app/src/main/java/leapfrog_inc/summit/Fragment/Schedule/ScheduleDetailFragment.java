package leapfrog_inc.summit.Fragment.Schedule;

import android.os.Bundle;
import android.support.v4.app.FragmentController;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import leapfrog_inc.summit.Fragment.BaseFragment;
import leapfrog_inc.summit.Fragment.Common.Dialog;
import leapfrog_inc.summit.Fragment.Common.Loading;
import leapfrog_inc.summit.Function.Constants;
import leapfrog_inc.summit.Function.DeviceUtility;
import leapfrog_inc.summit.Function.PicassoUtility;
import leapfrog_inc.summit.Function.SaveData;
import leapfrog_inc.summit.Http.Requester.AccountRequester;
import leapfrog_inc.summit.Http.Requester.ScheduleRequester;
import leapfrog_inc.summit.Http.Requester.UserRequester;
import leapfrog_inc.summit.R;

/**
 * Created by Leapfrog-Software on 2018/05/08.
 */

public class ScheduleDetailFragment extends BaseFragment {

    private ScheduleRequester.ScheduleData mScheduleData;

    public void set(ScheduleRequester.ScheduleData scheduleData) {
        mScheduleData = scheduleData;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {

        View view = inflater.inflate(R.layout.fragment_schedule_detail, null);

        initContents(view);
        initAction(view);

        return view;
    }

    private void initContents(View view) {

        UserRequester.UserData myUserData = UserRequester.getInstance().myUserData();
        if (myUserData == null) return;

        // 画像
        ImageView scheduleImageView = (ImageView)view.findViewById(R.id.scheduleImageView);
        ViewGroup.LayoutParams params = scheduleImageView.getLayoutParams();
        params.height = DeviceUtility.getWindowSize(getActivity()).x * 2 / 3;
        scheduleImageView.setLayoutParams(params);
        PicassoUtility.getScheduleImage(getActivity(), Constants.ScheduleImageDirectory + mScheduleData.id, (ImageView)view.findViewById(R.id.scheduleImageView));

        // 参加者リスト
        LinearLayout memberLayout = (LinearLayout)view.findViewById(R.id.memberLayout);
        memberLayout.addView(createPaddingView(16));
        ArrayList<UserRequester.UserData> userList = UserRequester.getInstance().queryReservedUser(mScheduleData.id);
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).userId.equals(SaveData.getInstance().userId)) continue;
            ImageView imageView = new ImageView(getActivity());
            float density = DeviceUtility.getDeviceDensity(getActivity());
            imageView.setLayoutParams(new ViewGroup.LayoutParams((int)(34 * density), (int)(34 * density)));
            memberLayout.addView(imageView);
            PicassoUtility.getUserImage(getActivity(), Constants.UserImageDirectory + userList.get(i).userId, imageView);

            memberLayout.addView(createPaddingView(8));
        }
        memberLayout.addView(createPaddingView(16));

        ((TextView)view.findViewById(R.id.memberCountTextView)).setText("(" + String.valueOf(userList.size()) + "名)");


        // 日時
        SimpleDateFormat format = new SimpleDateFormat("M月d日 kk:mm〜");
        format.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        String datetime = format.format(mScheduleData.datetime.getTime());
        ((TextView)view.findViewById(R.id.dateTextView)).setText(datetime);

        // プロバイダー
        ((TextView)view.findViewById(R.id.providerTextView)).setText(mScheduleData.provider);

        // 詳細
        ((TextView)view.findViewById(R.id.descriptionTextView)).setText(mScheduleData.description);

        // 参加条件
        ((TextView)view.findViewById(R.id.filterTextView)).setText(mScheduleData.filter);

        // 予約ボタン
        Button reserveButton = (Button)view.findViewById(R.id.reserveButton);
        if (myUserData.reserves.contains(mScheduleData.id)) {
            reserveButton.setBackgroundResource(R.drawable.shape_schedule_reserve_button_disable);
            reserveButton.setText("予約済みです");
            reserveButton.setEnabled(false);
        } else {
            Date current = new Date();
            Date scheduleDate = mScheduleData.datetime.getTime();
            long timeDiff = scheduleDate.getTime() - current.getTime();
            if (timeDiff <= 60 * 60 * 1000) {
                reserveButton.setBackgroundResource(R.drawable.shape_schedule_reserve_button_disable);
                reserveButton.setText("予約可能時間を過ぎています");
                reserveButton.setEnabled(false);
            }
        }
    }

    private View createPaddingView(int width) {
        View view = new View(getActivity());
        float density = DeviceUtility.getDeviceDensity(getActivity());
        view.setLayoutParams(new ViewGroup.LayoutParams((int)((float)width * density), 1));
        return view;
    }

    private void initAction(View view) {

        ((ImageButton)view.findViewById(R.id.backButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popFragment(AnimationType.horizontal);
            }
        });

        ((Button)view.findViewById(R.id.reserveButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickReserve();
            }
        });
    }

    private void onClickReserve() {

        UserRequester.UserData myUserData = UserRequester.getInstance().myUserData();
        if (myUserData == null) return;

        if ((myUserData.nameLast.length() == 0) || (myUserData.nameFirst.length() == 0) || (myUserData.kanaLast.length() == 0) || (myUserData.kanaFirst.length() == 0)) {
            Dialog.show(getActivity(), Dialog.Style.error, "プロフィールに未設定項目があります", "プロフィールを編集してください", null);
            return;
        }

        myUserData.reserves.add(mScheduleData.id);

        Loading.start(getActivity());

        AccountRequester.updateUser(myUserData, new AccountRequester.UpdateUserCallback() {
            @Override
            public void didReceiveData(boolean result) {
                if (result) {
                    UserRequester.getInstance().fetch(new UserRequester.UserRequesterCallback() {
                        @Override
                        public void didReceiveData(boolean result) {

                            Loading.stop(getActivity());

                            Dialog.show(getActivity(), Dialog.Style.success, "確認", "予約が完了しました", new Dialog.DialogCallback() {
                                @Override
                                public void didClose() {
                                    popFragment(AnimationType.horizontal);
                                }
                            });

                            View view = getView();
                            if (view == null) return;
                            Button reserveButton = (Button)view.findViewById(R.id.reserveButton);
                            reserveButton.setBackgroundResource(R.drawable.shape_schedule_reserve_button_disable);
                            reserveButton.setText("予約済みです");
                            reserveButton.setEnabled(false);
                        }
                    });
                } else {
                    Loading.stop(getActivity());
                    Dialog.show(getActivity(), Dialog.Style.error, "エラー", "通信に失敗しました", null);
                }
            }
        });
    }
}
