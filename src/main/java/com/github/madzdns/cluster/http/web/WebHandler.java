package com.github.madzdns.cluster.http.web;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import com.github.madzdns.cluster.http.codec.HttpMinaEncoder;
import com.github.madzdns.cluster.http.HTTPCode;
import com.github.madzdns.cluster.http.HTTPRequest;
import com.github.madzdns.cluster.http.HTTPResponse;
import com.github.madzdns.cluster.http.codec.HttpMinaDecoder;
import com.github.madzdns.cluster.http.web.annotation.ContentType;
import com.github.madzdns.cluster.http.web.annotation.RequestParam;
import com.github.madzdns.cluster.http.web.annotation.RouteMap;
import com.github.madzdns.cluster.http.web.helper.QueryString;

import com.github.madzdns.cluster.http.HTTPMessage;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eognl.EOgnl;
import eognl.EOgnlContext;
import eognl.EOgnlRuntime;
import eognl.OgnlContext;
import eognl.OgnlException;
import com.github.madzdns.cluster.core.api.ProtoInfo;
import com.github.madzdns.cluster.core.api.net.NetProvider;
import com.github.madzdns.cluster.core.api.net.ssl.server.filter.MinaSslFilter;
import com.github.madzdns.cluster.core.backend.node.dynamic.ServiceInfo;
import com.github.madzdns.cluster.core.codec.ServerCodecFactory;

public class WebHandler extends IoHandlerAdapter {

    public static class ParamsContainer {

        private RequestParam requestParam;
        private Type paramType;

        public ParamsContainer(RequestParam requestParam, Type paramType) {

            this.requestParam = requestParam;
            this.paramType = paramType;
        }

        public RequestParam getRequestParam() {

            return requestParam;
        }

        public void setRequestParam(RequestParam requestParam) {

            this.requestParam = requestParam;
        }

        public Type getParamType() {

            return paramType;
        }

        public void setParamType(Type paramType) {

            this.paramType = paramType;
        }
    }

    private final static String METHOD_MAP = "method";
    private final static String PARAMS_MAP = "params";
    private final static String TYPE_MAP = "object";
    private final static String CONTENT_TYPE_MAP = "content";

    private Logger log = LoggerFactory.getLogger(getClass());
    private Map<String, Map<String, Object>> routeMaps =
            new HashMap<String, Map<String, Object>>();

    public WebHandler(List<Class<? extends WebController>> maps) {

        for (Iterator<Class<? extends WebController>> it = maps.iterator(); it.hasNext(); ) {

            final Class<? extends WebController> map = it.next();

            RouteMap routemap = map.getAnnotation(RouteMap.class);

            if (routemap == null)

                continue;

            String[] routemapValues = routemap.value();

            if (routemapValues == null)

                continue;

            for (String controlerRoute : routemapValues) {

                final Method[] methods = map.getMethods();

                for (final Method method : methods) {

                    String route = null;

                    routemap = method.getAnnotation(RouteMap.class);

                    if (routemap == null)

                        continue;

                    Type[] genericTypes = method.getGenericParameterTypes();

                    if (routemap.value() == null)

                        continue;

                    String[] routeMapValues = routemap.value();

                    for (int x = 0; x < routeMapValues.length; x++) {

                        String methodRoute = routeMapValues[x];

                        Annotation[][] params = method.getParameterAnnotations();

                        final List<ParamsContainer> paramsContainerList = new ArrayList<ParamsContainer>();

                        final ContentType contentType = method.getAnnotation(ContentType.class);

                        for (int i = 0; i < params.length; i++) {

                            Annotation[] paz = params[i];

                            for (int j = 0; j < paz.length; j++) {

                                Annotation pa = paz[j];
                                if (pa instanceof RequestParam) {

                                    paramsContainerList.add(new ParamsContainer((RequestParam) pa,
                                            genericTypes[i]));
                                }
                            }
                        }
                        try {

                            route = controlerRoute + methodRoute;
                            log.info("Adding route map:{}", route);
                            routeMaps.put(route, new HashMap<String, Object>() {

                                private static final long serialVersionUID = 1L;

                                {
                                    put(METHOD_MAP, method);
                                    put(TYPE_MAP, map);
                                    put(PARAMS_MAP, paramsContainerList);
                                    put(CONTENT_TYPE_MAP, contentType);
                                }
                            });

                        } catch (Exception e) {

                            log.error("", e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {

        SSLContext ssl = NetProvider.getServerTLSContext(true);

        if (ssl != null) {

            //we use incomming port as the default port number
            final short DEFAULT_PORT_NUM = (short) ((InetSocketAddress) session.getLocalAddress()).getPort();
            MinaSslFilter sslFilter = new MinaSslFilter(ssl, session, new ProtoInfo(ServiceInfo.TCP, "HTTP_WEB", DEFAULT_PORT_NUM), false);
            session.getFilterChain().addLast(MinaSslFilter.NAME, sslFilter);
        }
        session.getFilterChain().addLast("webservice_coder",
                new ProtocolCodecFilter(new ServerCodecFactory(new HttpMinaDecoder(), new HttpMinaEncoder())));
    }

    @Override
    public void messageReceived(IoSession session, Object message)
            throws Exception {

        if (message instanceof HTTPRequest) {

            HTTPRequest request = (HTTPRequest) message;
            HTTPResponse response = new HTTPResponse();

            String host = request.getHost();
            String path = request.getUrl();
            String query = null;

            int idx = -1;

            if (request.getMethod() == HTTPMessage.Method.POST) {

                StringBuffer sb = request.getBufferAsStringBuffer();

                if (sb.length() > 0) {

                    sb.insert(0, "?");
                    query = sb.toString();
                    idx = 0;
                }
            } else {

                idx = path.indexOf("?");

                if (idx != -1) {

                    query = path.substring(idx);
                    path = path.substring(0, idx);
                }

            }

            log.debug("url:{}, host:{}, path:{}, query:{}", request.getUrl(), request.getHost(), path, query);

            if (host == null) {

                log.error("Host directive is not set");

                response.setCode(HTTPCode.BadRequest);

                response.setCommonHeaders();

                session.write(response);

                return;
            }

            Map<String, Object> route = routeMaps.get(path);

            if (route != null) {

                List<Object> orderdParams = new ArrayList<Object>();

                Map<String, List<String>> params = null;

                if (idx != -1)

                    params = new QueryString().parsQuery(query);

                Object methodParamsList = route.get(PARAMS_MAP);

                if (methodParamsList instanceof List<?>) {

                    Object expression = null;

                    EOgnlContext context = null;

                    @SuppressWarnings("unchecked")
                    List<ParamsContainer> methodParams = (List<ParamsContainer>) methodParamsList;

                    if (params != null) {

                        for (int i = 0; i < methodParams.size(); i++) {

                            try {

                                ParamsContainer paramsContainer = methodParams.get(i);

                                RequestParam rp = paramsContainer.getRequestParam();

                                Type type = paramsContainer.getParamType();

                                List<String> values = params.get(rp.name());

                                if (values == null || values.size() == 0) {

                                    Object value = null;

                                    if (EOgnlRuntime.isPrimitiveOrWrapper(type)) {

                                        value = EOgnlRuntime.getPrimitivesDefult(type);
                                    } else if (type instanceof Class<?> &&
                                            String.class.isAssignableFrom((Class<?>) type) &&
                                            !rp.isNullable()) {

                                        value = "";
                                    }

                                    orderdParams.add(value);

                                    continue;

                                }

                                if (type instanceof ParameterizedType) {

                                    ParameterizedType ptype = (ParameterizedType) type;

                                    Class<?> cls = (Class<?>) ptype.getRawType();

                                    Object obj = EOgnlRuntime.createProperObject(cls, cls.getComponentType());

                                    context = new EOgnlContext(OgnlContext.CONF_AUTO_EXPAND |
                                            OgnlContext.CONF_INIT_NULLS |
                                            OgnlContext.CONF_INIT_UNKNOWN |
                                            OgnlContext.CONF_UNKNOWN_TO_LITERAL |
                                            OgnlContext.CONF_CAST_PRIMITIVES,
                                            ptype.getActualTypeArguments());

                                    for (int j = 0; j < values.size(); j++) {

                                        expression = EOgnl.parseExpression(values.get(j));

                                        EOgnl.getValue(expression, context, obj);

                                    }

                                    orderdParams.add(obj);
                                } else if (type instanceof Class<?>) {

                                    Class<?> cls = (Class<?>) type;

                                    if (cls.isArray()) {

                                        Object obj = EOgnlRuntime.createProperObject(cls, cls.getComponentType());

                                        context = new EOgnlContext(OgnlContext.CONF_AUTO_EXPAND |
                                                OgnlContext.CONF_INIT_NULLS |
                                                OgnlContext.CONF_INIT_UNKNOWN |
                                                OgnlContext.CONF_UNKNOWN_TO_LITERAL |
                                                OgnlContext.CONF_CAST_PRIMITIVES);

                                        for (int j = 0; j < values.size(); j++) {

                                            expression = EOgnl.parseExpression(values.get(j));

                                            EOgnl.getValue(expression, context, obj);

                                            obj = EOgnl.getExpandedRootArray(context);

                                        }

                                        orderdParams.add(obj);

                                    } else if (EOgnlRuntime.isPrimitiveOrWrapper(type)) {

                                        Object obj = EOgnlRuntime.createProperObject(cls, null);

                                        context = new EOgnlContext(OgnlContext.CONF_INIT_NULLS |
                                                OgnlContext.CONF_UNKNOWN_TO_LITERAL |
                                                OgnlContext.CONF_CAST_PRIMITIVES);

                                        expression = EOgnl.parseExpression(values.get(0));

                                        obj = EOgnl.getValue(expression, context, obj);

                                        orderdParams.add(obj);
                                    } else {

                                        Object obj = EOgnlRuntime.createProperObject(cls, cls.getComponentType());


                                        context = new EOgnlContext(OgnlContext.CONF_AUTO_EXPAND |
                                                OgnlContext.CONF_INIT_NULLS |
                                                OgnlContext.CONF_INIT_UNKNOWN |
                                                OgnlContext.CONF_UNKNOWN_TO_LITERAL |
                                                OgnlContext.CONF_CAST_PRIMITIVES);

                                        expression = EOgnl.parseExpression(values.get(0));

                                        if (String.class.isAssignableFrom(cls)) {

                                            obj = EOgnl.getValue(expression, context, obj);
                                        } else {

                                            EOgnl.getValue(expression, context, obj);

                                        }

                                        orderdParams.add(obj);
                                    }

                                }

                            } catch (OgnlException e) {
                                log.error("", e);
                            }
                        }
                    } else {

                        Object value = null;

                        for (int i = 0; i < methodParams.size(); i++) {

                            ParamsContainer paramsContainer = methodParams.get(i);

                            Type type = paramsContainer.getParamType();

                            value = null;

                            if (EOgnlRuntime.isPrimitiveOrWrapper(type)) {

                                value = EOgnlRuntime.getPrimitivesDefult(type);
                            }

                            orderdParams.add(value);

                        }
                    }
                }

                Object theMethod = route.get(METHOD_MAP);

                Method method = null;

                Object obj = null;

                boolean error = true;

                if (theMethod instanceof Method) {

                    method = (Method) theMethod;

                    Class<?> controler = (Class<?>) route.get(TYPE_MAP);

                    log.debug("Call clazz {} for method {} with params:{}", controler.getName(), method.getName(), orderdParams);

                    try {

                        Object instance = controler.newInstance();

                        ((WebController) instance).setLocal((InetSocketAddress) session.getLocalAddress());

                        ((WebController) instance).setRemote((InetSocketAddress) session.getRemoteAddress());


                        obj = method.invoke(instance, orderdParams.toArray());

                        error = false;
                    } catch (Exception e) {
                        log.error("", e);
                    }

                }

                if (obj != null) {

                    log.debug("params list {}", methodParamsList);

                    Object contentType = route.get(CONTENT_TYPE_MAP);

                    if (contentType != null)

                        response.setHeader("Content-Type", ((ContentType) contentType).value());

                    response.appendToBuffer(obj.toString());
                }

                if (!error)

                    response.setCode(HTTPCode.OK);

                else {

                    response.setCode(HTTPCode.BadRequest);
                }

            } else {

                response.setCode(HTTPCode.NotFound);
            }

            response.setCommonHeaders();

            session.write(response);
        }
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause)
            throws Exception {

        if (cause instanceof IOException) {

            InetSocketAddress p = ((InetSocketAddress) session.getRemoteAddress());
            log.error("{} by {}:{}", cause.getMessage(),
                    p.getAddress().getHostAddress(), p.getPort());
            return;
        }

        log.error("", cause);

        NetProvider.closeMinaSession(session, true);
    }
}
