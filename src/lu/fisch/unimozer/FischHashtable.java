/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package lu.fisch.unimozer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

/**
 *
 * @author robertfisch
 */
public class FischHashtable<K,V> implements Map<K, V>
{
    Vector<Hashdata> elements = new Vector<Hashdata>();

    @Override
    public int size()
    {
        return elements.size();
    }

    @Override
    public boolean isEmpty()
    {
        return elements.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        boolean found = false;
        for(int i=0;i<elements.size();i++)
            if (elements.get(i).key.equals(key)) found=true;
        return found;
    }

    @Override
    public boolean containsValue(Object value)
    {
        boolean found = false;
        for(int i=0;i<elements.size();i++)
            if (elements.get(i).value.equals(value)) found=true;
        return found;
    }

    @Override
    public V get(Object key)
    {
        for(int i=0;i<elements.size();i++)
            if (elements.get(i).key.equals(key)) return elements.get(i).value;
        return null;
    }

    @Override
    public V put(K key, V value)
    {
        elements.add(new Hashdata(key, value));
        return value;
    }

    @Override
    public V remove(Object key)
    {
        V value = null;
        for(int i=elements.size()-1;i>=0;i--)
            if (elements.get(i).key.equals(key))
            {
                value = elements.get(i).value;
                elements.remove(i);
            }
        return value;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear()
    {
        elements.clear();
    }

    @Override
    public Set<K> keySet()
    {
        Set<K> set = new HashSet<K>();
        for(int i=0;i<elements.size();i++)
            set.add(elements.get(i).key);
        return set;
    }

    @Override
    public Collection<V> values()
    {
        Vector<V> collection = new Vector<V>();
        for(int i=0;i<elements.size();i++)
            collection.add(elements.get(i).value);
        return collection;
    }

    @Override
    public Set<Entry<K, V>> entrySet()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }



    class Hashdata
    {
        public K key;
        public V value;
        
        public Hashdata(K key, V value)
        {
            this.key=key;
            this.value=value;
        }
    }



}
