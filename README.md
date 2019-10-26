# Aether

> 网络抓包、网络检测

## 原理

依赖[NetBare](https://github.com/MegatronKing/NetBare). NetBare是一款网络包拦截和注入框架，可以实现抓包、屏蔽包、改包等各种强大功能。NetBare核心是基于VPN技术，将网络包转发到本地代理服务器，再通过虚拟网关（VirtualGateway）进行拦截分发。在设计上，虚拟网关层是完全对外开放的，开发者可以自由定义虚拟网关，也可以使用NetBare内部已实现的虚拟网关进行网络包的处理。


## 鸣谢

* [MegatronKing](https://github.com/MegatronKing) - [NetBare](https://github.com/MegatronKing/NetBare) 
