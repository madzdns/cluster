package rtmp.handler;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.red5.server.api.IAttributeStore;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScope;

public class FakeClientForConnection implements IClient{
	
	private long creationTime = new Date().getTime();
	
	String id;
	
	public FakeClientForConnection(String id) {
		
		this.id = id;
	}

	@Override
	public Set<String> getAttributeNames() {

		return null;
	}

	@Override
	public Map<String, Object> getAttributes() {

		return null;
	}

	@Override
	public boolean setAttribute(String name, Object value) {

		return false;
	}

	@Override
	public boolean setAttributes(Map<String, Object> values) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setAttributes(IAttributeStore values) {

		return false;
	}

	@Override
	public Object getAttribute(String name) {

		return null;
	}

	@Override
	public Object getAttribute(String name, Object defaultValue) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasAttribute(String name) {

		return false;
	}

	@Override
	public boolean removeAttribute(String name) {

		return false;
	}

	@Override
	public void removeAttributes() {
		
	}

	@Override
	public int size() {

		return 0;
	}

	@Override
	public String getId() {

		return this.id;
	}

	@Override
	public long getCreationTime() {

		return this.creationTime;
	}

	@Override
	public Collection<IScope> getScopes() {

		return null;
	}

	@Override
	public Set<IConnection> getConnections() {

		return null;
	}

	@Override
	public Set<IConnection> getConnections(IScope scope) {

		return null;
	}

	@Override
	public void disconnect() {

	}

	@Override
	public void setPermissions(IConnection conn, Collection<String> permissions) {

	}

	@Override
	public Collection<String> getPermissions(IConnection conn) {

		return null;
	}

	@Override
	public boolean hasPermission(IConnection conn, String permissionName) {

		return false;
	}

	@Override
	public void checkBandwidth() {
		
	}

	@Override
	public Map<String, Object> checkBandwidthUp(Object[] params) {

		return null;
	}

	@Override
	public boolean isBandwidthChecked() {

		return false;
	}

}
