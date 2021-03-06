package com.mine.musharing.audio;

import android.util.Log;

import com.mine.musharing.models.Msg;
import com.mine.musharing.models.Music;
import com.mine.musharing.models.Playlist;
import com.mine.musharing.models.SerializableList;
import com.mine.musharing.models.User;
import com.mine.musharing.requestTasks.ReceiveTask;
import com.mine.musharing.requestTasks.RequestTaskListener;
import com.mine.musharing.requestTasks.SendTask;
import com.mine.musharing.utils.ParseUtil;
import com.mine.musharing.utils.UserUtil;

import java.util.List;

/**
 * 播放列表的"管理员"
 *
 * 储存这当前使用的播放列表；
 * 并完成和别人的播放列表的同步。
 */
public class MusicListHolder {

    private static final String TAG = "MusicListHolder";

    /**
     * 当前登录的用户
     */
    private User user = UserUtil.playlistFakeUser;

    /**
     * 歌曲列表
     */
    private final SerializableList<Music> musicList = new SerializableList<>();   // 初始化以防止列表在没有完成从服务器下载时被调用导致 a null object reference

    private Playlist playlist;

    private String id;

    private Playlist sendingPlaylist;

    private boolean changed = true;

    public boolean receiveFlag = true;

    private MusicListHolder() {
        setPlaylist(new Playlist());
    }

    /**
     * 单例Holder
     */
    private static class SingletonHolder {
        private static MusicListHolder instance = new MusicListHolder();
    }

    /**
     * 获取 MusicListHolder 的单例
     * @return MusicListHolder 的单例
     */
    public static MusicListHolder getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * 设置当前用户
     * @param user 当前用户
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * 获取当前播放列表对应的 Playlist 对象
     * @return
     */
    public Playlist getPlaylist() {
        playlist.setType(Msg.TYPE_PLAYLIST);
        playlist.setFromUser(user);
        playlist.commit();
        return playlist;
    }

    /**
     * 从 Playlist 对象重置当前播放列表
     * @param playlist
     */
    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
        setList(playlist.getMusicList());
        Log.d(TAG, "setPlaylist:\n playlist: " + playlist + "\n musicList: " + musicList);
    }

    /**
     * 向房间中的其他人发送当前自己的播放列表
     */
    public void postPlaylist() {
        sendPlaylist(getPlaylist());
    }

    /**
     * 设置播放列表
     * @param ms @code{List<Music>} 的播放列表
     */
    private void setList(List<Music> ms) {
        musicList.clear();
        musicList.addAll(ms);
    }

    /**
     * 获取播放列表
     * @return @code{List<Music>} 的播放列表
     */
    public SerializableList<Music> getMusicList() {
        return musicList;
    }

    /**
     * 处理收到的 Playlist 消息，更新当前列表
     * @param msg
     * @return true if list changed
     */
    public boolean handlePlaylistMsg(Msg msg) {
        Playlist newPlaylist = ParseUtil.playlistContentParse(msg.getContent());

        if (newPlaylist != null && !newPlaylist.getId().equals(this.playlist.getId()) && !newPlaylist.getMusicList().isEmpty()) {
            Log.d(TAG, "handlePlaylistMsg: use new playlist from " + msg.getFromUid());
            setPlaylist(newPlaylist);
            return true;
        }

        return false;
    }

    /**
     * 处理接收到的 Playlist 消息
     * @param msg 接收到的 Playlist 消息
     */
    public void handleQueryPlaylistMsg(Msg msg) {
        if (!msg.getFromUid().equals(user.getUid())) {
            Log.d(TAG, "handleQueryPlaylistMsg");
            postPlaylist();
        }
    }

    /**
     * 发送播放列表
     * @param pl Playlist对象表示的播放列表
     */
    private void sendPlaylist(Playlist pl) {

        Log.d(TAG, "sendPlaylist: " + pl);

        if (sendingPlaylist != null && pl.getContent().equals(sendingPlaylist.getContent())) {
            // 可能是用户快速连点按钮造成的大量相同数据，不重复发送
            return;
        }

        new SendTask(new RequestTaskListener<String>() {
            @Override
            public void onStart() {
                sendingPlaylist = pl;
            }

            @Override
            public void onSuccess(String s) {}

            @Override
            public void onFailed(String error) {}

            @Override
            public void onFinish(String s) {
                sendingPlaylist = null;
            }
        }).execute(user.getUid(), pl.toString());
    }

    /**
     * 向房间中的其他人询问播放列表
     */
    public void queryPlaylist() {

        Msg msg = new Msg(Msg.TYPE_QUERY_PLAYLIST, user, "");
        new SendTask(new RequestTaskListener<String>() {
            @Override
            public void onStart() {}

            @Override
            public void onSuccess(String s) {}

            @Override
            public void onFailed(String error) {}

            @Override
            public void onFinish(String s) {}
        }).execute(user.getUid(), msg.toString());
    }

    /**
     * 临时的消息获取
     *
     * <em>⚠注意：这个方法只能在 MusicChatActivity 活跃前被启用；
     * 当 MusicChatActivity 开始工作时，这个方法必须被停用。</em>
     *
     */
    public void refreshMsgs() {

        if (receiveFlag) {
            new ReceiveTask(new RequestTaskListener<List<Msg>>() {
                @Override
                public void onStart() {}

                @Override
                public void onSuccess(List<Msg> newMsgs) {
                    if (!newMsgs.isEmpty()) {
                        Log.d(TAG, "MusicListHolder refreshMsgs: new Msgs: " + newMsgs);
                    }
                    for (Msg msg : newMsgs) {
                        switch (msg.getType()) {
                            case Msg.TYPE_TEXT:
                                break;
                            case Msg.TYPE_PLAYER_ASYNC:
                                break;
                            case Msg.TYPE_RECORD:
                                break;
                            case Msg.TYPE_PLAYLIST:
                                MusicListHolder.getInstance().handlePlaylistMsg(msg);
                                break;
                            case Msg.TYPE_QUERY_PLAYLIST:
                                MusicListHolder.getInstance().handleQueryPlaylistMsg(msg);
                        }
                    }
                }

                @Override
                public void onFailed(String error) {}

                @Override
                public void onFinish(String s) {}

            }).execute(user.getUid());
        }
    }

}
