<template>
	<view class="hxy-home" :data-theme="theme">
		<view class="safe-top" :style="{ height: safeTop + 'px' }"></view>

		<view class="hero-card">
			<view class="hero-title">荷小悦 · 健康养护</view>
			<view class="hero-subtitle">到店调理 · 放松理疗 · 预约优先</view>
			<view class="hero-search" @click="go('/pages/goods/goods_search/index')">
				<text class="hero-search-text">搜索项目 / 门店 / 资讯</text>
			</view>
		</view>

		<view class="panel quick-panel">
			<view class="panel-title">常用服务</view>
			<view class="quick-grid">
				<view class="quick-item" v-for="item in quickActions" :key="item.name" @click="go(item.path)">
					<image class="quick-icon" :src="urlDomain + item.icon" mode="aspectFit"></image>
					<view class="quick-text">{{ item.name }}</view>
				</view>
			</view>
		</view>

		<view class="panel" v-if="bannerList.length">
			<view class="panel-head">
				<view class="panel-title">活动推荐</view>
			</view>
			<swiper class="banner-swiper" circular autoplay interval="3500" duration="500">
				<swiper-item v-for="item in bannerList" :key="item.id || item.pic">
					<image class="banner-image" :src="item.pic" mode="aspectFill" @click="onBannerTap(item)"></image>
				</swiper-item>
			</swiper>
		</view>

		<view class="panel">
			<view class="panel-head">
				<view class="panel-title">推荐项目</view>
				<view class="panel-more" @click="go('/pages/goods/goods_list/index?title=推荐项目')">更多</view>
			</view>

			<view v-if="serviceList.length" class="service-list">
				<view class="service-item" v-for="item in serviceList" :key="item.id" @click="toServiceDetail(item.id)">
					<image class="service-cover" :src="resolveImage(item.image)"></image>
					<view class="service-body">
						<view class="service-name line1">{{ item.storeName || '到店养护项目' }}</view>
						<view class="service-meta">{{ item.sales || 0 }}人已预约</view>
						<view class="service-price">¥{{ item.price || '0.00' }}</view>
					</view>
				</view>
			</view>

			<view v-else class="empty-wrap">
				<image class="empty-icon" :src="urlDomain + 'crmebimage/perset/staticImg/noShopper.png'"></image>
				<view class="empty-text">暂无项目，稍后再试</view>
			</view>
		</view>

		<view class="hotline" v-if="consumerHotline">
			<view class="hotline-label">服务电话</view>
			<view class="hotline-phone" @click="callPhone">{{ consumerHotline }}</view>
		</view>

		<pageFooter></pageFooter>
	</view>
</template>

<script>
	import Cache from '@/utils/cache';
	import pageFooter from '@/components/pageFooter/index.vue';
	import { getIndexData, getImageDomain } from '@/api/api.js';
	import { getProductslist } from '@/api/store.js';

	const app = getApp();
	const TAB_PAGES = [
		'/pages/index/index',
		'/pages/goods_cate/goods_cate',
		'/pages/order_addcart/order_addcart',
		'/pages/user/index'
	];

	function getStatusBarHeight() {
		try {
			if (typeof uni.getWindowInfo === 'function') {
				const win = uni.getWindowInfo();
				if (win && win.statusBarHeight) return win.statusBarHeight;
			}
		} catch (e) {}
		try {
			const info = uni.getSystemInfoSync();
			return info.statusBarHeight || 20;
		} catch (e) {
			return 20;
		}
	}

	export default {
		components: {
			pageFooter
		},
		data() {
			return {
				theme: app.globalData.theme,
				safeTop: getStatusBarHeight(),
				urlDomain: Cache.get('imgHost') || 'https://admin.hexiaoyue.com/',
				bannerList: [],
				serviceList: [],
				consumerHotline: '',
				quickActions: [
					{ name: '快速预约', icon: 'crmebimage/perset/staticImg/time.png', path: '/pages/goods/goods_details_store/index' },
					{ name: '门店列表', icon: 'crmebimage/perset/staticImg/address.png', path: '/pages/goods/goods_details_store/index' },
					{ name: '我的订单', icon: 'crmebimage/perset/staticImg/order1.png', path: '/pages/users/order_list/index' },
					{ name: '会员中心', icon: 'crmebimage/perset/staticImg/member.png', path: '/pages/infos/user_vip/index' },
					{ name: '优惠券', icon: 'crmebimage/perset/staticImg/coupon.png', path: '/pages/users/user_coupon/index' },
					{ name: '健康档案', icon: 'crmebimage/perset/staticImg/avatar.png', path: '/pages/infos/user_info/index' },
					{ name: '在线客服', icon: 'crmebimage/perset/staticImg/customer.png', path: '/pages/users/kefu/index' },
					{ name: '商品分类', icon: 'crmebimage/perset/staticImg/noSearch.png', path: '/pages/goods_cate/goods_cate' }
				]
			};
		},
		onLoad() {
			this.bootstrap();
		},
		onShow() {
			this.theme = app.globalData.theme;
		},
		onPullDownRefresh() {
			this.bootstrap().finally(() => uni.stopPullDownRefresh());
		},
		methods: {
			async bootstrap() {
				await this.initImageDomain();
				await Promise.all([this.loadIndex(), this.loadServices()]);
			},
			async initImageDomain() {
				try {
					const res = await getImageDomain();
					const domain = this.normalizeDomain(res && res.data ? res.data : '');
					if (domain) {
						this.urlDomain = domain;
						Cache.set('imgHost', domain);
						return;
					}
				} catch (e) {}
				this.urlDomain = this.normalizeDomain(this.urlDomain);
			},
			async loadIndex() {
				try {
					const res = await getIndexData();
					const data = res && res.data ? res.data : {};
					this.bannerList = data.banner || [];
					this.consumerHotline = data.consumerHotline || '';
					if (!Cache.get('imgHost') && data.logoUrl && data.logoUrl.indexOf('crmebimage') > -1) {
						const domain = this.normalizeDomain(data.logoUrl.split('crmebimage')[0]);
						if (domain) {
							this.urlDomain = domain;
							Cache.set('imgHost', domain);
						}
					}
				} catch (e) {
					this.bannerList = [];
				}
			},
			async loadServices() {
				try {
					const res = await getProductslist({ page: 1, limit: 6 });
					this.serviceList = (res && res.data && res.data.list) ? res.data.list : [];
				} catch (e) {
					this.serviceList = [];
				}
			},
			normalizeDomain(domain) {
				if (!domain) return '';
				return domain.endsWith('/') ? domain : `${domain}/`;
			},
			resolveImage(path) {
				if (!path) return `${this.urlDomain}crmebimage/perset/staticImg/noShopper.png`;
				if (path.indexOf('http://') === 0 || path.indexOf('https://') === 0) return path;
				return `${this.urlDomain}${String(path).replace(/^\/+/, '')}`;
			},
			toServiceDetail(id) {
				if (!id) return;
				this.go(`/pages/goods/goods_details/index?id=${id}`);
			},
			callPhone() {
				if (!this.consumerHotline) return;
				uni.makePhoneCall({ phoneNumber: this.consumerHotline });
			},
			onBannerTap(item) {
				const route = this.normalizeRoute(item && item.url ? item.url : '');
				if (!route) return;
				this.go(route);
			},
			normalizeRoute(url) {
				if (!url || /^https?:\/\//.test(url)) return '';
				let route = url.startsWith('/') ? url : `/${url}`;
				route = route.replace('/pages/news_details/', '/pages/news/news_details/');
				route = route.replace('/pages/news_list/', '/pages/news/news_list/');
				route = route.replace('/pages/goods_list/', '/pages/goods/goods_list/');
				route = route.replace('/pages/goods_details/', '/pages/goods/goods_details/');
				route = route.replace('/pages/order_details/', '/pages/order/order_details/');
				return route;
			},
			go(path) {
				if (!path) return;
				const cleanPath = this.normalizeRoute(path) || path;
				const basePath = cleanPath.split('?')[0];
				if (TAB_PAGES.indexOf(basePath) > -1) {
					uni.switchTab({
						url: basePath,
						fail: () => {
							uni.showToast({ title: '页面暂不可用', icon: 'none' });
						}
					});
					return;
				}
				uni.navigateTo({
					url: cleanPath,
					fail: () => {
						uni.showToast({ title: '页面建设中', icon: 'none' });
					}
				});
			}
		}
	};
</script>

<style scoped lang="scss">
	.hxy-home {
		min-height: 100vh;
		background: linear-gradient(180deg, #eef9f5 0%, #f6f8fb 45%, #ffffff 100%);
		padding: 0 24rpx 140rpx;
		box-sizing: border-box;
	}

	.safe-top {
		width: 100%;
	}

	.hero-card {
		background: linear-gradient(135deg, #1f9d8e 0%, #5dc47a 100%);
		border-radius: 28rpx;
		padding: 30rpx;
		box-shadow: 0 20rpx 40rpx rgba(31, 157, 142, 0.2);
		margin-bottom: 22rpx;
	}

	.hero-title {
		font-size: 40rpx;
		font-weight: 700;
		color: #ffffff;
	}

	.hero-subtitle {
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.92);
		margin-top: 8rpx;
	}

	.hero-search {
		margin-top: 24rpx;
		height: 72rpx;
		border-radius: 36rpx;
		background: rgba(255, 255, 255, 0.2);
		display: flex;
		align-items: center;
		padding: 0 28rpx;
		box-sizing: border-box;
	}

	.hero-search-text {
		font-size: 26rpx;
		color: rgba(255, 255, 255, 0.95);
	}

	.panel {
		background: #ffffff;
		border-radius: 24rpx;
		padding: 24rpx;
		margin-bottom: 20rpx;
		box-shadow: 0 8rpx 24rpx rgba(39, 56, 94, 0.07);
	}

	.panel-head {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 16rpx;
	}

	.panel-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #1b2a3a;
	}

	.panel-more {
		font-size: 24rpx;
		color: #1f9d8e;
	}

	.quick-grid {
		display: grid;
		grid-template-columns: repeat(4, 1fr);
		gap: 18rpx 10rpx;
	}

	.quick-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 12rpx 6rpx;
	}

	.quick-icon {
		width: 68rpx;
		height: 68rpx;
		margin-bottom: 10rpx;
	}

	.quick-text {
		font-size: 22rpx;
		color: #334155;
	}

	.banner-swiper {
		height: 250rpx;
		border-radius: 18rpx;
		overflow: hidden;
	}

	.banner-image {
		width: 100%;
		height: 100%;
	}

	.service-list {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 18rpx;
	}

	.service-item {
		background: #f8fbff;
		border-radius: 18rpx;
		overflow: hidden;
	}

	.service-cover {
		width: 100%;
		height: 180rpx;
		background: #edf2f7;
	}

	.service-body {
		padding: 14rpx;
	}

	.service-name {
		font-size: 26rpx;
		font-weight: 600;
		color: #14213d;
	}

	.service-meta {
		font-size: 22rpx;
		color: #64748b;
		margin-top: 6rpx;
	}

	.service-price {
		font-size: 30rpx;
		font-weight: 700;
		color: #e11d48;
		margin-top: 10rpx;
	}

	.empty-wrap {
		padding: 32rpx 0 12rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.empty-icon {
		width: 220rpx;
		height: 220rpx;
		opacity: 0.82;
	}

	.empty-text {
		margin-top: 8rpx;
		font-size: 24rpx;
		color: #94a3b8;
	}

	.hotline {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 20rpx 26rpx;
		background: #fff;
		border-radius: 18rpx;
		margin-bottom: 24rpx;
	}

	.hotline-label {
		font-size: 24rpx;
		color: #64748b;
	}

	.hotline-phone {
		font-size: 28rpx;
		font-weight: 700;
		color: #0f766e;
	}

	.line1 {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
</style>
