# hello-aggregator
dropbyme
在该插件中，首先通过rpc实现了获得topo的功能，可以得到下层topo的信息，其次是自定义了
一个notification，虽然可以成功监听到，但是在merge或put linksong的时候，会发生IllegalArgumentException方法参数错误
虽然仔细检查，但目前仍然没有发现报错原因，还待解决。
