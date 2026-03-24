<template>
  <s-layout title="技师动态" navbar="inner">
    <view class="page-wrap ss-p-24">
      <view class="hero-card ss-m-b-20">
        <view class="hero-title">技师动态</view>
        <view class="hero-desc">查看技师近期服务动态、为喜欢的内容点赞，并提交新的服务反馈。</view>
      </view>

      <s-empty
        v-if="!state.loading && state.list.length === 0"
        icon="/static/data-empty.png"
        text="当前还没有技师动态"
      />

      <view v-for="item in state.list" :key="item.postId" class="feed-card ss-m-b-20">
        <view class="ss-flex ss-row-between ss-col-center ss-m-b-12">
          <view>
            <view class="feed-title">{{ item.title || '服务动态' }}</view>
            <view class="feed-meta">动态编号 {{ item.postId }} · 技师 {{ item.technicianId }}</view>
          </view>
          <view class="feed-date">{{ formatDate(item.publishedAt) }}</view>
        </view>
        <image v-if="item.coverUrl" class="feed-cover ss-m-b-12" :src="item.coverUrl" mode="aspectFill" />
        <view class="feed-content ss-m-b-16">{{ item.content }}</view>
        <view class="action-row">
          <button class="action-btn ss-reset-button" @tap="onToggleLike(item)">
            {{ item.liked ? '取消点赞' : '点赞' }} {{ item.likeCount || 0 }}
          </button>
          <button class="action-btn ghost ss-reset-button" @tap="prepareComment(item)">
            发表评论 {{ item.commentCount || 0 }}
          </button>
        </view>
      </view>

      <view v-if="state.commentPostId" class="comment-card">
        <view class="comment-title">发表评论</view>
        <textarea
          v-model="state.commentContent"
          class="comment-textarea"
          maxlength="200"
          placeholder="说说本次服务体验"
        />
        <view class="ss-flex ss-row-between ss-col-center ss-m-t-12">
          <view class="comment-tip">当前评论会进入 REVIEWING 队列</view>
          <view>
            <button class="mini-btn ghost ss-reset-button" @tap="cancelComment">取消</button>
            <button class="mini-btn ss-reset-button" @tap="submitComment">提交</button>
          </view>
        </view>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app';
  import TechnicianFeedApi from '@/sheep/api/trade/technicianFeed';

  const state = reactive({
    loading: false,
    storeId: 0,
    technicianId: 0,
    list: [],
    pageNo: 1,
    pageSize: 10,
    lastId: null,
    hasMore: true,
    commentPostId: null,
    commentContent: '',
  });

  function formatDate(value) {
    if (!value) return '--';
    return String(value).replace('T', ' ').slice(0, 16);
  }

  function normalizeRows(rows) {
    return (rows || []).map((item) => ({
      ...item,
      liked: Boolean(item.liked),
      likeCount: Number(item.likeCount || 0),
      commentCount: Number(item.commentCount || 0),
    }));
  }

  async function loadFeedPage(reset = false) {
    if (reset) {
      state.list = [];
      state.pageNo = 1;
      state.lastId = null;
      state.hasMore = true;
    }
    if (!state.hasMore && !reset) {
      return;
    }
    state.loading = true;
    const { code, data } = await TechnicianFeedApi.getFeedPage({
      storeId: state.storeId,
      technicianId: state.technicianId || undefined,
      pageNo: state.pageNo,
      pageSize: state.pageSize,
      lastId: state.lastId || undefined,
    });
    state.loading = false;
    if (code !== 0) {
      return;
    }
    const rows = normalizeRows(data?.list);
    state.list = reset ? rows : state.list.concat(rows);
    state.hasMore = Boolean(data?.hasMore);
    state.lastId = data?.nextCursor || (rows.length ? rows[rows.length - 1].postId : state.lastId);
    state.pageNo += 1;
  }

  async function onToggleLike(item) {
    const nextLiked = !item.liked;
    const { code, data } = await TechnicianFeedApi.toggleLike({
      postId: item.postId,
      action: nextLiked ? 1 : 0,
      clientToken: `feed-like-${item.postId}-${Date.now()}`,
    });
    if (code !== 0) {
      return;
    }
    item.liked = Boolean(data?.liked);
    item.likeCount = Number(data?.likeCount || 0);
  }

  function prepareComment(item) {
    state.commentPostId = item.postId;
    state.commentContent = '';
  }

  function cancelComment() {
    state.commentPostId = null;
    state.commentContent = '';
  }

  async function submitComment() {
    if (!state.commentPostId) {
      return;
    }
    if (!state.commentContent.trim()) {
      uni.showToast({ title: '请输入评论内容', icon: 'none' });
      return;
    }
    const { code, data } = await TechnicianFeedApi.createComment({
      postId: state.commentPostId,
      content: state.commentContent.trim(),
      clientToken: `feed-comment-${state.commentPostId}-${Date.now()}`,
    });
    if (code !== 0) {
      return;
    }
    const target = state.list.find((item) => item.postId === state.commentPostId);
    if (target) {
      target.commentCount = Number(target.commentCount || 0) + 1;
    }
    uni.showToast({
      title: data?.status === 'REVIEWING' ? '评论已提交，等待审核' : '评论已提交',
      icon: 'none',
    });
    cancelComment();
  }

  onLoad((options) => {
    state.storeId = Number(options.storeId || 0);
    state.technicianId = Number(options.technicianId || options.id || 0);
    loadFeedPage(true);
  });

  onPullDownRefresh(async () => {
    await loadFeedPage(true);
    uni.stopPullDownRefresh();
  });

  onReachBottom(async () => {
    await loadFeedPage(false);
  });
</script>

<style scoped lang="scss">
  .page-wrap {
    min-height: 100vh;
    background: #f7f7f7;
  }

  .hero-card,
  .feed-card,
  .comment-card {
    padding: 28rpx;
    border-radius: 24rpx;
    background: #ffffff;
    box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.06);
  }

  .hero-title,
  .comment-title,
  .feed-title {
    font-size: 30rpx;
    font-weight: 700;
    color: #243b53;
  }

  .hero-desc,
  .feed-meta,
  .feed-date,
  .comment-tip,
  .feed-content {
    margin-top: 12rpx;
    font-size: 24rpx;
    line-height: 1.7;
    color: #6b7c93;
  }

  .feed-cover {
    width: 100%;
    height: 280rpx;
    border-radius: 20rpx;
    background: #f3f4f6;
  }

  .action-row {
    display: flex;
    gap: 16rpx;
  }

  .action-btn,
  .mini-btn {
    padding: 0 28rpx;
    height: 72rpx;
    border-radius: 999rpx;
    background: #f59e0b;
    color: #fff;
    font-size: 24rpx;
  }

  .action-btn.ghost,
  .mini-btn.ghost {
    background: #fff7e6;
    color: #ad6800;
  }

  .comment-textarea {
    width: 100%;
    height: 180rpx;
    margin-top: 16rpx;
    padding: 20rpx;
    border-radius: 20rpx;
    background: #f7f7f7;
    font-size: 26rpx;
    box-sizing: border-box;
  }
</style>
