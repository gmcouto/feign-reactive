package reactivefeign.webclient.client;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

class WebReactiveHttpResponse<P extends Publisher<?>> implements ReactiveHttpResponse<P>{

	private ReactiveHttpRequest reactiveRequest;
	private final ClientResponse clientResponse;
	private final Type returnPublisherType;
	private final ParameterizedTypeReference returnActualType;
	private final boolean wrapOnEntity;


	WebReactiveHttpResponse(ReactiveHttpRequest reactiveRequest,
							ClientResponse clientResponse,
							Type returnPublisherType, ParameterizedTypeReference returnActualType) {
		this.reactiveRequest = reactiveRequest;
		this.clientResponse = clientResponse;
		this.returnPublisherType = returnPublisherType;
    if (returnActualType.getType() instanceof ParameterizedTypeImpl
        && ((ParameterizedTypeImpl) returnActualType.getType())
            .getRawType()
            .equals(ResponseEntity.class)) {
      this.returnActualType =
          ParameterizedTypeReference.forType(
              ((ParameterizedTypeImpl) returnActualType.getType()).getActualTypeArguments()[0]);
      wrapOnEntity = true;
    } else {
      this.returnActualType = returnActualType;
      wrapOnEntity = false;
    }
	}

	@Override
	public ReactiveHttpRequest request() {
		return reactiveRequest;
	}

	@Override
	public int status() {
		return clientResponse.statusCode().value();
	}

	@Override
	public Map<String, List<String>> headers() {
		return clientResponse.headers().asHttpHeaders();
	}

	@Override
	public P body() {
		if (returnPublisherType == Mono.class) {
      if (wrapOnEntity) {
        return (P) clientResponse.toEntity(returnActualType);
        }
			else {
			  return (P)clientResponse.bodyToMono(returnActualType);
      }
		} else if(returnPublisherType == Flux.class){
			return (P)clientResponse.bodyToFlux(returnActualType);
		} else {
			throw new IllegalArgumentException("Unknown returnPublisherType: " + returnPublisherType);
		}
	}

	@Override
	public Mono<byte[]> bodyData() {
		Flux<DataBuffer> response = clientResponse.body(BodyExtractors.toDataBuffers());
		return DataBufferUtils.join(response)
				.map(dataBuffer -> {
					byte[] result = new byte[dataBuffer.readableByteCount()];
					dataBuffer.read(result);
					DataBufferUtils.release(dataBuffer);
					return result;
				})
				.defaultIfEmpty(new byte[0]);
	}
}
